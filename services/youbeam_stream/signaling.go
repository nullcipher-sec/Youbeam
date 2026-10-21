package main

import (
	"context"
	"crypto/ecdsa"
	"encoding/json"
	"log/slog"
	"net/http"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/pion/webrtc/v4"
)

// maxOfferBytes limita el cuerpo de /offer: una oferta SDP real ocupa unos pocos KB.
const maxOfferBytes = 64 << 10

type OfferRequest struct {
	SDP    string `json:"sdp"`
	Ticket string `json:"ticket"`
}

func HandleOffer(w http.ResponseWriter, r *http.Request, api *webrtc.API, repo VideoRepository, storage StorageService, ticketKey *ecdsa.PublicKey) {
	if r.Method != http.MethodPost {
		http.Error(w, "Método no permitido", http.StatusMethodNotAllowed)
		return
	}

	var req OfferRequest
	r.Body = http.MaxBytesReader(w, r.Body, maxOfferBytes)
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "JSON inválido", http.StatusBadRequest)
		return
	}

	// 1. Validar JWT (la clave se carga una sola vez al arrancar)
	claims := jwt.MapClaims{}
	// Solo ES256, con exp, emitido por el Core y dirigido a este servidor: un ticket firmado
	// para otro servicio no se acepta aquí
	token, err := jwt.ParseWithClaims(req.Ticket, claims, func(t *jwt.Token) (interface{}, error) {
		return ticketKey, nil
	}, jwt.WithValidMethods([]string{"ES256"}), jwt.WithExpirationRequired(),
		jwt.WithIssuer(TicketIssuer), jwt.WithAudience(TicketAudience))

	if err != nil || !token.Valid {
		slog.Warn("Ticket JWT Inválido", "error", err)
		http.Error(w, "Ticket inválido", http.StatusUnauthorized)
		return
	}

	videoID, okVid := claims["vid"].(string)
	jti, okJti := claims["jti"].(string)
	exp, okExp := claims["exp"].(float64)
	if !okVid || !okJti || !okExp {
		http.Error(w, "Ticket malformado", http.StatusUnauthorized)
		return
	}

	// 2. Anti-Replay
	if err := repo.SaveUsedTicket(jti, time.Unix(int64(exp), 0)); err != nil {
		slog.Warn("Intento de Replay detectado", "jti", jti)
		http.Error(w, "Ticket ya utilizado", http.StatusForbidden)
		return
	}

	// 3. WebRTC
	pc, err := api.NewPeerConnection(webrtc.Configuration{})
	if err != nil {
		http.Error(w, "Error creando PeerConnection", http.StatusInternalServerError)
		return
	}
	// NOTA: NO HAY defer pc.Close() AQUÍ. El cierre se maneja abajo.

	videoTrack, _ := webrtc.NewTrackLocalStaticSample(webrtc.RTPCodecCapability{MimeType: webrtc.MimeTypeVP8}, "video", "pion")
	audioTrack, _ := webrtc.NewTrackLocalStaticSample(webrtc.RTPCodecCapability{MimeType: webrtc.MimeTypeOpus}, "audio", "pion")
	pc.AddTrack(videoTrack)
	pc.AddTrack(audioTrack)

	if err := pc.SetRemoteDescription(webrtc.SessionDescription{Type: webrtc.SDPTypeOffer, SDP: req.SDP}); err != nil {
		slog.Warn("SDP inválido recibido", "err", err)
		pc.Close() // Cierre inmediato si falla
		http.Error(w, "SDP inválido", http.StatusBadRequest)
		return
	}

	answer, err := pc.CreateAnswer(nil)
	if err != nil {
		slog.Error("Error creando answer", "err", err)
		pc.Close() // Cierre inmediato si falla
		http.Error(w, "Error interno", http.StatusInternalServerError)
		return
	}

	gatherComplete := webrtc.GatheringCompletePromise(pc)
	if err := pc.SetLocalDescription(answer); err != nil {
		slog.Error("Error estableciendo local description", "err", err)
		pc.Close() // Cierre inmediato si falla
		http.Error(w, "Error interno", http.StatusInternalServerError)
		return
	}
	<-gatherComplete

	ctx, cancel := context.WithCancel(context.Background())

	pc.OnConnectionStateChange(func(state webrtc.PeerConnectionState) {
		if state == webrtc.PeerConnectionStateConnected {
			slog.Info("Navegador conectado", "video_id", videoID)
			go StreamMedia(ctx, repo, storage, videoID, videoTrack, audioTrack)
		}

		// [CORRECCIÓN CRÍTICA]: El cierre depende del ciclo de vida de la conexión, no del handler
		if state == webrtc.PeerConnectionStateDisconnected ||
			state == webrtc.PeerConnectionStateFailed ||
			state == webrtc.PeerConnectionStateClosed {
			slog.Info("Cliente desconectado o fallo. Cerrando PeerConnection y liberando recursos.", "video_id", videoID)
			cancel()   // Detiene los hilos de video/audio
			pc.Close() // Libera memoria
		}
	})

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(pc.LocalDescription())
}
