package main

import (
	"context"
	"encoding/json"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/pion/webrtc/v4"
)

type OfferRequest struct {
	SDP    string `json:"sdp"`
	Ticket string `json:"ticket"`
}

func HandleOffer(w http.ResponseWriter, r *http.Request, api *webrtc.API, repo VideoRepository, storage StorageService) {
	var req OfferRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "JSON inválido", http.StatusBadRequest)
		return
	}

	// 1. Validar JWT
	pubKeyPEM, _ := os.ReadFile("jwt_public.pem")
	key, _ := jwt.ParseECPublicKeyFromPEM(pubKeyPEM)
	claims := jwt.MapClaims{}
	token, err := jwt.ParseWithClaims(req.Ticket, claims, func(t *jwt.Token) (interface{}, error) {
		return key, nil
	})

	if err != nil || !token.Valid {
		slog.Warn("Ticket JWT Inválido", "error", err)
		http.Error(w, "Ticket inválido", http.StatusUnauthorized)
		return
	}

	videoID := claims["vid"].(string)
	jti := claims["jti"].(string)
	exp := int64(claims["exp"].(float64))

	// 2. Anti-Replay
	if err := repo.SaveUsedTicket(jti, time.Unix(exp, 0)); err != nil {
		slog.Warn("Intento de Replay detectado", "jti", jti)
		http.Error(w, "Ticket ya utilizado", http.StatusForbidden)
		return
	}
	slog.Info("Ticket validado", "video_id", videoID, "jti", jti)

	// 3. WebRTC
	pc, _ := api.NewPeerConnection(webrtc.Configuration{})
	videoTrack, _ := webrtc.NewTrackLocalStaticSample(webrtc.RTPCodecCapability{MimeType: webrtc.MimeTypeVP8}, "video", "pion")
	audioTrack, _ := webrtc.NewTrackLocalStaticSample(webrtc.RTPCodecCapability{MimeType: webrtc.MimeTypeOpus}, "audio", "pion")
	pc.AddTrack(videoTrack)
	pc.AddTrack(audioTrack)

	ctx, cancel := context.WithCancel(context.Background())

	pc.OnConnectionStateChange(func(state webrtc.PeerConnectionState) {
		if state == webrtc.PeerConnectionStateConnected {
			slog.Info("Navegador conectado vía WebRTC", "video_id", videoID)

			// [MEJORA] Telemetría WebRTC
			go func() {
				ticker := time.NewTicker(5 * time.Second)
				defer ticker.Stop()
				for {
					select {
					case <-ctx.Done():
						return
					case <-ticker.C:
						stats := pc.GetStats()
						for _, s := range stats {
							if outbound, ok := s.(webrtc.OutboundRTPStreamStats); ok {
								if outbound.Kind == "video" {
									slog.Info("Telemetría", "paquetes_enviados", outbound.PacketsSent, "bitrate_kbps", outbound.TargetBitrate/1000)
								}
							}
						}
					}
				}
			}()

			go StreamMedia(ctx, repo, storage, videoID, videoTrack, audioTrack)
		}
		if state == webrtc.PeerConnectionStateDisconnected || state == webrtc.PeerConnectionStateFailed {
			slog.Info("🔌 Navegador desconectado", "video_id", videoID)
			cancel()
		}
	})

	pc.SetRemoteDescription(webrtc.SessionDescription{Type: webrtc.SDPTypeOffer, SDP: req.SDP})
	answer, _ := pc.CreateAnswer(nil)
	gatherComplete := webrtc.GatheringCompletePromise(pc)
	pc.SetLocalDescription(answer)
	<-gatherComplete

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(pc.LocalDescription())
}
