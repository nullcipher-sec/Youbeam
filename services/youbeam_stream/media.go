package main

import (
	"context"
	"log/slog"
	"sync"
	"time"

	"github.com/pion/webrtc/v4"
	"github.com/pion/webrtc/v4/pkg/media"
	"github.com/pion/webrtc/v4/pkg/media/ivfreader"
	"github.com/pion/webrtc/v4/pkg/media/oggreader"
)

func StreamMedia(ctx context.Context, repo VideoRepository, storage StorageService, videoID string, videoTrack, audioTrack *webrtc.TrackLocalStaticSample) {

	wrappedDEK, err := repo.GetWrappedDEK(videoID)
	if err != nil {
		slog.Error("Error obteniendo DEK", "err", err)
		return
	}
	dek, err := UnsealDEK(wrappedDEK, videoID)
	if err != nil {
		slog.Error("Error descifrando DEK con HPKE", "err", err)
		return
	}

	encVideo, err := storage.GetEncryptedVideo(videoID)
	if err != nil {
		slog.Error("Error abriendo video", "err", err)
		return
	}
	defer encVideo.Close() // Se cierra pase lo que pase

	decVideoReader, err := GetDecryptingReader(encVideo, dek, videoID, true)
	if err != nil {
		return
	}
	ivf, _, err := ivfreader.NewWith(decVideoReader)
	if err != nil {
		return
	}

	encAudio, err := storage.GetEncryptedAudio(videoID)
	if err != nil {
		slog.Warn("No hay audio, transmitiendo solo video")
		return
	}
	defer encAudio.Close() // Se cierra pase lo que pase

	decAudioReader, err := GetDecryptingReader(encAudio, dek, videoID, false)
	if err != nil {
		return
	}
	ogg, _, err := oggreader.NewWith(decAudioReader)
	if err != nil {
		slog.Warn("El audio no es Opus válido, transmitiendo solo video")
	}

	slog.Info("Iniciando transmisión...")

	var wg sync.WaitGroup
	videoFinished := make(chan struct{})

	// Goroutine Video
	wg.Add(1)
	go func() {
		defer wg.Done()
		defer close(videoFinished)
		for {
			select {
			case <-ctx.Done():
				return
			default:
				frame, _, err := ivf.ParseNextFrame()
				if err != nil {
					slog.Info("Fin del video o error GCM")
					return
				}
				videoTrack.WriteSample(media.Sample{Data: frame, Duration: 33 * time.Millisecond})
				time.Sleep(33 * time.Millisecond)
			}
		}
	}()

	// Goroutine Audio
	if ogg != nil {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for {
				select {
				case <-ctx.Done():
					return
				case <-videoFinished:
					slog.Info("Audio detenido porque el video terminó")
					return
				default:
					page, _, err := ogg.ParseNextPage()
					if err != nil {
						slog.Info("Fin del audio o error GCM")
						return
					}
					audioTrack.WriteSample(media.Sample{Data: page, Duration: 20 * time.Millisecond})
					time.Sleep(20 * time.Millisecond)
				}
			}
		}()
	}

	wg.Wait()
	slog.Info("Transmisión terminada de forma natural. Recursos liberados.")
}
