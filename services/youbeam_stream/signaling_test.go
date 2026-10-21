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

// newTestKey genera un par P-256 como el del Core (jwt_private.pem / jwt_public.pem)
func newTestKey(t *testing.T) *ecdsa.PrivateKey {
	t.Helper()
	privateKey, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatalf("Error generando llave de prueba: %v", err)
	}
	return privateKey
}

// signTicket firma un ticket con los claims que emite el Core; extra sobrescribe o añade claims
func signTicket(t *testing.T, key *ecdsa.PrivateKey, extra jwt.MapClaims) string {
	t.Helper()
	claims := jwt.MapClaims{
		"sub": "anonymous",
		"vid": "video-123",
		"jti": "jti-" + t.Name(),
		"iss": TicketIssuer,
		"aud": TicketAudience,
		"exp": time.Now().Add(1 * time.Minute).Unix(),
	}
	for k, v := range extra {
		claims[k] = v
	}
	ticket, err := jwt.NewWithClaims(jwt.SigningMethodES256, claims).SignedString(key)
	if err != nil {
		t.Fatalf("Error firmando ticket: %v", err)
	}
	return ticket
}

func postOffer(t *testing.T, body string, repo VideoRepository, key *ecdsa.PublicKey) int {
	t.Helper()
	req := httptest.NewRequest(http.MethodPost, "/offer", strings.NewReader(body))
	w := httptest.NewRecorder()
	HandleOffer(w, req, webrtc.NewAPI(), repo, &LocalStorage{}, key)
	return w.Code
}

func TestHandleOfferRejectsInvalidTicket(t *testing.T) {
	key := newTestKey(t)

	code := postOffer(t, `{"sdp": "v=0...", "ticket": "ticket-falso"}`, &MockDatabase{}, &key.PublicKey)

	if code != http.StatusUnauthorized {
		t.Errorf("Se esperaba código 401, se obtuvo %d", code)
	}
}

func TestHandleOfferWithValidTicketButInvalidSDP(t *testing.T) {
	key := newTestKey(t)
	body := `{"sdp": "esto-es-basura", "ticket": "` + signTicket(t, key, nil) + `"}`

	code := postOffer(t, body, &MockDatabase{}, &key.PublicKey)

	// El servidor NO debe colgarse. Debe devolver un 400 Bad Request.
	if code != http.StatusBadRequest {
		t.Errorf("Se esperaba código 400 (SDP inválido), se obtuvo %d", code)
	}
}

func TestHandleOfferRejectsTicketsNotIssuedForThisServer(t *testing.T) {
	key := newTestKey(t)
	cases := map[string]jwt.MapClaims{
		"otra audiencia":       {"aud": "otro-servicio"},
		"otro emisor":          {"iss": "atacante"},
		"vencido":              {"exp": time.Now().Add(-1 * time.Second).Unix()},
		"firmado con otra key": nil,
	}

	for name, extra := range cases {
		t.Run(name, func(t *testing.T) {
			signer := key
			if extra == nil {
				signer = newTestKey(t)
			}
			body := `{"sdp": "v=0", "ticket": "` + signTicket(t, signer, extra) + `"}`

			if code := postOffer(t, body, &MockDatabase{}, &key.PublicKey); code != http.StatusUnauthorized {
				t.Errorf("Se esperaba 401, se obtuvo %d", code)
			}
		})
	}
}

func TestHandleOfferRejectsReplayedTicket(t *testing.T) {
	key := newTestKey(t)
	repo := &MockDatabase{}
	body := `{"sdp": "esto-es-basura", "ticket": "` + signTicket(t, key, nil) + `"}`

	postOffer(t, body, repo, &key.PublicKey)
	code := postOffer(t, body, repo, &key.PublicKey)

	if code != http.StatusForbidden {
		t.Errorf("Se esperaba 403 (replay), se obtuvo %d", code)
	}
}

func TestHandleOfferRejectsOversizedBodyAndOtherMethods(t *testing.T) {
	key := newTestKey(t)

	huge := `{"sdp": "` + strings.Repeat("a", maxOfferBytes+1) + `", "ticket": "x"}`
	if code := postOffer(t, huge, &MockDatabase{}, &key.PublicKey); code != http.StatusBadRequest {
		t.Errorf("Se esperaba 400 para un cuerpo gigante, se obtuvo %d", code)
	}

	req := httptest.NewRequest(http.MethodGet, "/offer", nil)
	w := httptest.NewRecorder()
	HandleOffer(w, req, webrtc.NewAPI(), &MockDatabase{}, &LocalStorage{}, &key.PublicKey)
	if w.Code != http.StatusMethodNotAllowed {
		t.Errorf("Se esperaba 405 para GET, se obtuvo %d", w.Code)
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
