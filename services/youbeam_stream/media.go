package main

import (
	"context"
	"log/slog"
	"time"

	"github.com/pion/webrtc/v4"
	"github.com/pion/webrtc/v4/pkg/media"
	"github.com/pion/webrtc/v4/pkg/media/ivfreader"
	"github.com/pion/webrtc/v4/pkg/media/oggreader"
)

func StreamMedia(ctx context.Context, repo VideoRepository, storage StorageService, videoID string, videoTrack, audioTrack *webrtc.TrackLocalStaticSample) {

	// [NOTA PARA INTEGRACIÓN CON MARCELO/JHONMAR]
	// Cuando tengan MinIO y Postgres listos, descomenten este bloque:
	/*
		wrappedDEK, err := repo.GetWrappedDEK(videoID)
		if err != nil { slog.Error("Error obteniendo DEK", "err", err); return }
		dek, err := UnsealDEK(wrappedDEK, videoID)
		if err != nil { slog.Error("Error HPKE", "err", err); return }
	*/

	// 1. Preparar Video
	encVideo, err := storage.GetEncryptedVideo(videoID)
	if err != nil {
		slog.Error("No se pudo abrir el video", "err", err)
		return
	}
	defer encVideo.Close()

	// [NOTA] Para producción: ivf, _, err := ivfreader.NewWith(decVideoReader)
	ivf, _, err := ivfreader.NewWith(encVideo)
	if err != nil {
		slog.Error("El archivo IVF no es válido", "err", err)
		return
	}

	// 2. Preparar Audio
	var ogg *oggreader.OggReader
	encAudio, errAudio := storage.GetEncryptedAudio(videoID)
	if errAudio == nil {
		defer encAudio.Close()

		// [NOTA] Para producción: ogg, _, errOgg = oggreader.NewWith(decAudioReader)
		var errOgg error
		ogg, _, errOgg = oggreader.NewWith(encAudio)
		if errOgg != nil {
			slog.Warn("El archivo OGG no es Opus válido. Se transmitirá solo video", "err", errOgg)
			ogg = nil
		} else {
			slog.Info("Audio Opus cargado correctamente")
		}
	}

	slog.Info("Iniciando envío de muestras VP8 + Opus")

	// Goroutine Video
	go func() {
		for {
			frame, _, err := ivf.ParseNextFrame()
			if err != nil {
				slog.Info("Fin del video")
				return
			}
			videoTrack.WriteSample(media.Sample{Data: frame, Duration: 33 * time.Millisecond})
			time.Sleep(33 * time.Millisecond)
		}
	}()

	// Goroutine Audio (en bucle)
	if ogg != nil {
		go func() {
			for {
				page, _, err := ogg.ParseNextPage()
				if err != nil {
					if err.Error() == "EOF" {
						encAudio.Close()
						newAudio, _ := storage.GetEncryptedAudio(videoID)
						ogg, _, _ = oggreader.NewWith(newAudio)
						continue
					}
					return
				}
				audioTrack.WriteSample(media.Sample{Data: page, Duration: 20 * time.Millisecond})
				time.Sleep(20 * time.Millisecond)
			}
		}()
	}

	<-ctx.Done()
}
