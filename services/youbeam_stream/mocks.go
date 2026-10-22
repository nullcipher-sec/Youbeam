package main

import (
	"fmt"
	"io"
	"os"
	"sync"
	"time"
)

// MockDatabase simula Postgres
type MockDatabase struct {
	usedTickets sync.Map
}

func (m *MockDatabase) SaveUsedTicket(jti string, exp time.Time) error {
	if _, loaded := m.usedTickets.LoadOrStore(jti, true); loaded {
		return fmt.Errorf("ticket duplicado") // Fuerza el anti-replay
	}
	return nil
}

func (m *MockDatabase) GetWrappedDEK(videoID string) ([]byte, error) {
	// Devolvemos una DEK falsa en texto plano para pruebas sin HPKE
	// En producción, esto vendría de Postgres (encriptado)
	return []byte("0123456789abcdef0123456789abcdef"), nil
}

// LocalStorage simula MinIO
type LocalStorage struct{}

func (l *LocalStorage) GetEncryptedVideo(videoID string) (io.ReadCloser, error) {
	return os.Open("output.ivf") // Lee el video directo de tu disco
}

func (l *LocalStorage) GetEncryptedAudio(videoID string) (io.ReadCloser, error) {
	return os.Open("output.ogg")
}
