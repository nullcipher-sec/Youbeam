package main

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/x509"
	"encoding/pem"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/pion/webrtc/v4"
)

// setupTestKeys genera una llave pública temporal para que las pruebas no fallen por falta de archivo
func setupTestKeys(t *testing.T) *ecdsa.PrivateKey {
	privateKey, _ := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	pubBytes, _ := x509.MarshalPKIXPublicKey(&privateKey.PublicKey)
	pubPem := pem.EncodeToMemory(&pem.Block{Type: "PUBLIC KEY", Bytes: pubBytes})

	err := os.WriteFile("jwt_public.pem", pubPem, 0644)
	if err != nil {
		t.Fatalf("Error creando llave de prueba: %v", err)
	}

	// Al terminar el test, borramos el archivo
	t.Cleanup(func() {
		os.Remove("jwt_public.pem")
	})

	return privateKey
}

func TestHandleOfferRejectsInvalidTicket(t *testing.T) {
	setupTestKeys(t)

	api := webrtc.NewAPI()
	body := `{"sdp": "v=0...", "ticket": "ticket-falso"}`
	req := httptest.NewRequest(http.MethodPost, "/offer", strings.NewReader(body))
	w := httptest.NewRecorder()

	HandleOffer(w, req, api, &MockDatabase{}, &LocalStorage{})

	if w.Code != http.StatusUnauthorized {
		t.Errorf("Se esperaba código 401, se obtuvo %d", w.Code)
	}
}

func TestHandleOfferWithValidTicketButInvalidSDP(t *testing.T) {
	privateKey := setupTestKeys(t)

	// Crear un ticket VÁLIDO
	claims := jwt.MapClaims{
		"vid": "video-123",
		"jti": "test-unique-jti",
		"exp": time.Now().Add(1 * time.Hour).Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodES256, claims)
	validTicket, _ := token.SignedString(privateKey)

	// Simular la petición con un Ticket Válido pero un SDP INVÁLIDO
	body := `{"sdp": "esto-es-basura", "ticket": "` + validTicket + `"}`
	req := httptest.NewRequest(http.MethodPost, "/offer", strings.NewReader(body))
	w := httptest.NewRecorder()

	api := webrtc.NewAPI()
	HandleOffer(w, req, api, &MockDatabase{}, &LocalStorage{})

	// El servidor NO debe colgarse. Debe devolver un 400 Bad Request.
	if w.Code != http.StatusBadRequest {
		t.Errorf("Se esperaba código 400 (SDP inválido), se obtuvo %d", w.Code)
	}
}
