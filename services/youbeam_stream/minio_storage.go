package main

import (
	"context"
	"io"

	"github.com/minio/minio-go/v7"
)

type MinioStorage struct {
	Client *minio.Client
}

func (m *MinioStorage) GetEncryptedVideo(videoID string) (io.ReadCloser, error) {
	return m.Client.GetObject(context.Background(), "videos", "videos/"+videoID+"/video.enc", minio.GetObjectOptions{})
}

func (m *MinioStorage) GetEncryptedAudio(videoID string) (io.ReadCloser, error) {
	return m.Client.GetObject(context.Background(), "videos", "videos/"+videoID+"/audio.enc", minio.GetObjectOptions{})
}
