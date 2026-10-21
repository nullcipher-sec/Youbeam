package main

import (
	"context"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/x509"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/pion/dtls/v3"
	"github.com/pion/ice/v4"
	"github.com/pion/webrtc/v4"
)

func main() {
	// 0. Inicializar Logging Estructurado (JSON o Texto)
	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	slog.SetDefault(logger)
	slog.Info("Iniciando YouBeam Streaming Server...")

	// 1. Generar llaves locales para pruebas (En producción se leen de /run/secrets)
	privateKey, _ := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	pubBytes, _ := x509.MarshalPKIXPublicKey(&privateKey.PublicKey)
	pubPem := pem.EncodeToMemory(&pem.Block{Type: "PUBLIC KEY", Bytes: pubBytes})
	os.WriteFile("jwt_public.pem", pubPem, 0644)

	// 2. Configurar Pion (DTLS-SRTP)
	s := webrtc.SettingEngine{}
	mux, _ := ice.NewMultiUDPMuxFromPort(8443)
	s.SetICEUDPMux(mux)
	s.SetAnsweringDTLSRole(webrtc.DTLSRoleServer)
	s.SetSRTPProtectionProfiles(dtls.SRTP_AEAD_AES_128_GCM)
	api := webrtc.NewAPI(webrtc.WithSettingEngine(s))

	// 3. Inyectar Dependencias (Usamos Mocks por ahora)
	var repo VideoRepository = &MockDatabase{}
	var storage StorageService = &LocalStorage{}

	// 4. Configurar Rutas HTTP
	muxHttp := http.NewServeMux()

	// 4.1. Healthcheck (Para Docker/Jhonmar)
	muxHttp.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	muxHttp.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "index.html")
	})

	muxHttp.HandleFunc("/get-test-ticket", func(w http.ResponseWriter, r *http.Request) {
		claims := jwt.MapClaims{
			"vid": "video-123",
			"jti": fmt.Sprintf("%d", time.Now().UnixNano()),
			"exp": time.Now().Add(60 * time.Second).Unix(),
		}
		token := jwt.NewWithClaims(jwt.SigningMethodES256, claims)
		tokenString, _ := token.SignedString(privateKey)
		json.NewEncoder(w).Encode(map[string]string{"ticket": tokenString})
	})

	muxHttp.HandleFunc("/offer", func(w http.ResponseWriter, r *http.Request) {
		HandleOffer(w, r, api, repo, storage)
	})

	// 5. Configurar Servidor con Graceful Shutdown
	srv := &http.Server{
		Addr:    ":8080",
		Handler: muxHttp,
	}

	go func() {
		slog.Info("Servidor escuchando en http://127.0.0.1:8080")
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("Error en el servidor HTTP", "err", err)
		}
	}()

	// Esperar señal de apagado
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)
	<-quit
	slog.Info("pagando servidor ordenadamente (Graceful Shutdown)...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		slog.Error("El servidor fue forzado a apagarse", "err", err)
	}
	slog.Info("Servidor apagado correctamente.")
}
