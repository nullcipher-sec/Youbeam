package main

import (
	"io"
	"time"
)

type VideoRepository interface {
	GetWrappedDEK(videoID string) ([]byte, error)
	SaveUsedTicket(jti string, exp time.Time) error
}

type StorageService interface {
	GetEncryptedVideo(videoID string) (io.ReadCloser, error)
	GetEncryptedAudio(videoID string) (io.ReadCloser, error)
}
