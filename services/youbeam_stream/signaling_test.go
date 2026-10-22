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
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/pion/webrtc/v4"
)

// setupTestKeys genera una llave pública temporal para que las pruebas no fallen por falta de archivo
func setupTestKeys(t *testing.T) *ecdsa.PrivateKey {
	privateKey, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatalf("Error creando llave de prueba: %v", err)
	}
	return privateKey
}

func TestHandleOfferRejectsInvalidTicket(t *testing.T) {
	privateKey := setupTestKeys(t)

	api := webrtc.NewAPI()
	body := `{"sdp": "v=0...", "ticket": "ticket-falso"}`
	req := httptest.NewRequest(http.MethodPost, "/offer", strings.NewReader(body))
	w := httptest.NewRecorder()

	HandleOffer(w, req, api, &MockDatabase{}, &LocalStorage{}, &privateKey.PublicKey)

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
		"iss": TicketIssuer,
		"aud": TicketAudience,
	}
	token := jwt.NewWithClaims(jwt.SigningMethodES256, claims)
	validTicket, _ := token.SignedString(privateKey)

	// Simular la petición con un Ticket Válido pero un SDP INVÁLIDO
	body := `{"sdp": "esto-es-basura", "ticket": "` + validTicket + `"}`
	req := httptest.NewRequest(http.MethodPost, "/offer", strings.NewReader(body))
	w := httptest.NewRecorder()

	api := webrtc.NewAPI()
	HandleOffer(w, req, api, &MockDatabase{}, &LocalStorage{}, &privateKey.PublicKey)

	// El servidor NO debe colgarse. Debe devolver un 400 Bad Request.
	if w.Code != http.StatusBadRequest {
		t.Errorf("Se esperaba código 400 (SDP inválido), se obtuvo %d", w.Code)
	}
}

func TestHandleOfferRejectsTicketForAnotherAudienceOrIssuer(t *testing.T) {
	privateKey := setupTestKeys(t)
	cases := map[string]jwt.MapClaims{
		"otra audiencia": {"iss": TicketIssuer, "aud": "otro-servicio"},
		"otro emisor":    {"iss": "atacante", "aud": TicketAudience},
		"sin aud ni iss": {},
	}

	for name, extra := range cases {
		t.Run(name, func(t *testing.T) {
			claims := jwt.MapClaims{"vid": "video-123", "jti": "jti-" + name, "exp": time.Now().Add(time.Minute).Unix()}
			for k, v := range extra {
				claims[k] = v
			}
			ticket, _ := jwt.NewWithClaims(jwt.SigningMethodES256, claims).SignedString(privateKey)
			req := httptest.NewRequest(http.MethodPost, "/offer", strings.NewReader(`{"sdp": "v=0", "ticket": "`+ticket+`"}`))
			w := httptest.NewRecorder()

			HandleOffer(w, req, webrtc.NewAPI(), &MockDatabase{}, &LocalStorage{}, &privateKey.PublicKey)

			if w.Code != http.StatusUnauthorized {
				t.Errorf("Se esperaba 401, se obtuvo %d", w.Code)
			}
		})
	}
}

func TestHandleOfferRejectsOversizedBody(t *testing.T) {
	privateKey := setupTestKeys(t)
	body := `{"sdp": "` + strings.Repeat("a", maxOfferBytes+1) + `", "ticket": "x"}`
	req := httptest.NewRequest(http.MethodPost, "/offer", strings.NewReader(body))
	w := httptest.NewRecorder()

	HandleOffer(w, req, webrtc.NewAPI(), &MockDatabase{}, &LocalStorage{}, &privateKey.PublicKey)

	if w.Code != http.StatusBadRequest {
		t.Errorf("Se esperaba 400 para un cuerpo de más de %d bytes, se obtuvo %d", maxOfferBytes, w.Code)
	}
}

func TestLoadTicketPublicKey(t *testing.T) {
	writePublicKey := func(t *testing.T, curve elliptic.Curve) string {
		privateKey, _ := ecdsa.GenerateKey(curve, rand.Reader)
		pubBytes, _ := x509.MarshalPKIXPublicKey(&privateKey.PublicKey)
		path := filepath.Join(t.TempDir(), "jwt_public.pem")
		os.WriteFile(path, pem.EncodeToMemory(&pem.Block{Type: "PUBLIC KEY", Bytes: pubBytes}), 0o600)
		return path
	}

	t.Run("carga la ruta de JWT_PUBLIC_KEY_PATH", func(t *testing.T) {
		path := writePublicKey(t, elliptic.P256())
		t.Setenv("JWT_PUBLIC_KEY_PATH", path)

		key, loaded, err := LoadTicketPublicKey()
		if err != nil || key == nil || loaded != path {
			t.Fatalf("Se esperaba cargar %s, se obtuvo path=%s err=%v", path, loaded, err)
		}
	})

	t.Run("rechaza claves que no son P-256", func(t *testing.T) {
		t.Setenv("JWT_PUBLIC_KEY_PATH", writePublicKey(t, elliptic.P384()))

		if _, _, err := LoadTicketPublicKey(); err == nil {
			t.Fatal("Se esperaba error para una clave P-384")
		}
	})
}
