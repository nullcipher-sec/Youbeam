package main

import (
	"io"
	"time"
)

// VideoRepository maneja la Base de Datos (Postgres)
type VideoRepository interface {
	GetWrappedDEK(videoID string) ([]byte, error)
	SaveUsedTicket(jti string, exp time.Time) error
}

// StorageService maneja los archivos (MinIO)
type StorageService interface {
	GetEncryptedVideo(videoID string) (io.ReadCloser, error)
	GetEncryptedAudio(videoID string) (io.ReadCloser, error)
}
