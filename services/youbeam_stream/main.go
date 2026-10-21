package main

import (
	"context"
	"database/sql"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/pion/dtls/v3"
	"github.com/pion/ice/v4"
	"github.com/pion/webrtc/v4"
	_ "github.com/lib/pq"
)

func main() {
	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	slog.SetDefault(logger)
	slog.Info("Iniciando YouBeam Streaming Server...")

	// Clave pública de tickets: la misma que firma el Core (deploy/secrets o Docker secret)
	ticketKey, keyPath, err := LoadTicketPublicKey()
	if err != nil {
		slog.Error("No se pudo cargar la clave pública de tickets", "err", err)
		os.Exit(1)
	}
	slog.Info("Clave pública de tickets cargada", "path", keyPath)

	// Configurar Pion (DTLS-SRTP)
	s := webrtc.SettingEngine{}
	mux, _ := ice.NewMultiUDPMuxFromPort(8443)
	s.SetICEUDPMux(mux)
	// En Docker el contenedor solo conoce su IP interna: PUBLIC_IP es la que debe anunciar en el SDP
	// para que el navegador pueda llegar al puerto UDP. En local, sin ella, se usan las IPs de la máquina.
	if publicIP := os.Getenv("PUBLIC_IP"); publicIP != "" {
		s.SetNAT1To1IPs([]string{publicIP}, webrtc.ICECandidateTypeHost)
		slog.Info("Anunciando IP pública en los candidatos ICE", "ip", publicIP)
	} else {
		slog.Warn("PUBLIC_IP no definida: se anuncian las IPs locales (válido solo fuera de Docker)")
	}
	s.SetAnsweringDTLSRole(webrtc.DTLSRoleServer)
	s.SetSRTPProtectionProfiles(dtls.SRTP_AEAD_AES_128_GCM)
	api := webrtc.NewAPI(webrtc.WithSettingEngine(s))

	// =========================================================================
	// INTEGRACIÓN INTELIGENTE (Mocks o Reales dependiendo del entorno)
	// =========================================================================
	var repo VideoRepository
	var storage StorageService

	dbURL := os.Getenv("DATABASE_URL")
	minioURL := os.Getenv("MINIO_ENDPOINT")

	if dbURL != "" && minioURL != "" {
		slog.Info("Conectando a Infraestructura Real (Postgres + MinIO)...")
		
		db, err := sql.Open("postgres", dbURL)
		if err != nil { slog.Error("Error DB", "err", err); os.Exit(1) }
		repo = &PostgresRepository{DB: db}

		minioClient, err := minio.New(minioURL, &minio.Options{
			Creds:  credentials.NewStaticV4(os.Getenv("MINIO_USER"), os.Getenv("MINIO_PASSWORD"), ""),
			Secure: false,
		})
		if err != nil { slog.Error("Error MinIO", "err", err); os.Exit(1) }
		storage = &MinioStorage{Client: minioClient}
	} else {
		slog.Warn("Variables de entorno no encontradas. Usando MOCKS para pruebas locales.")
		repo = &MockDatabase{}
		storage = &LocalStorage{}
	}
	// =========================================================================

	muxHttp := http.NewServeMux()

	// Healthcheck para Docker
	muxHttp.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	// Endpoint de señalización
	muxHttp.HandleFunc("/offer", func(w http.ResponseWriter, r *http.Request) {
		HandleOffer(w, r, api, repo, storage, ticketKey)
	})

	// Puerto configurable: en local el Core ya ocupa el 8080
	addr := os.Getenv("STREAM_HTTP_ADDR")
	if addr == "" {
		addr = ":8080"
	}
	srv := &http.Server{
		Addr:    addr,
		Handler: muxHttp,
	}

	go func() {
		slog.Info("Servidor escuchando", "addr", addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("Fallo crítico en el servidor HTTP (¿Puerto ocupado?)", "err", err)
			os.Exit(1) 
		}
	}()

	// Esperar señal de apagado (Graceful Shutdown)
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)
	<-quit
	slog.Info("Apagando servidor ordenadamente (Graceful Shutdown)...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		slog.Error("El servidor fue forzado a apagarse", "err", err)
	}
	slog.Info("Servidor apagado correctamente.")
}