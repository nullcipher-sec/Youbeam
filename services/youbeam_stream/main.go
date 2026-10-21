package main

import (
	"context"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/x509"
	"database/sql"
	"encoding/pem"
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

	// Si no estamos en Docker, generamos la llave local para pruebas
	if _, err := os.Stat("/run/secrets/jwt_public.pem"); os.IsNotExist(err) {
		slog.Warn("No se encontró llave en secrets, generando una local para pruebas...")
		privateKey, _ := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
		pubBytes, _ := x509.MarshalPKIXPublicKey(&privateKey.PublicKey)
		pubPem := pem.EncodeToMemory(&pem.Block{Type: "PUBLIC KEY", Bytes: pubBytes})
		os.WriteFile("jwt_public.pem", pubPem, 0644)
	}

	// Configurar Pion (DTLS-SRTP)
	s := webrtc.SettingEngine{}
	mux, _ := ice.NewMultiUDPMuxFromPort(8443)
	s.SetICEUDPMux(mux)
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

	// Servir el HTML de prueba local (opcional)
	muxHttp.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "index.html")
	})

	// Endpoint de señalización
	muxHttp.HandleFunc("/offer", func(w http.ResponseWriter, r *http.Request) {
		HandleOffer(w, r, api, repo, storage)
	})

	srv := &http.Server{
		Addr:    ":8080",
		Handler: muxHttp,
	}

	go func() {
		slog.Info("Servidor escuchando en http://0.0.0.0:8080")
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