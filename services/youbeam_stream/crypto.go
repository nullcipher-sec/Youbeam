package main

import (
	"bytes"
	"fmt"
	"io"
	"os"

	"github.com/tink-crypto/tink-go/v2/hybrid"
	"github.com/tink-crypto/tink-go/v2/insecurecleartextkeyset"
	"github.com/tink-crypto/tink-go/v2/keyset"
	"github.com/tink-crypto/tink-go/v2/streamingaead/subtle"
)

// UnsealDEK descifra la llave (DEK) usando HPKE.
func UnsealDEK(wrappedDEK []byte, videoID string) ([]byte, error) {
	// Busca en /run/secrets (Docker) o en local (Pruebas)
	path := "/run/secrets/hpke_private.json"
	if _, err := os.Stat(path); os.IsNotExist(err) {
		path = "hpke_private.json"
	}

	privKeyJSON, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("no se encontró hpke_private.json: %w", err)
	}

	reader := keyset.NewJSONReader(bytes.NewReader(privKeyJSON))
	handle, err := insecurecleartextkeyset.Read(reader)
	if err != nil {
		return nil, fmt.Errorf("error leyendo el keyset HPKE: %w", err)
	}

	hybridDecrypt, err := hybrid.NewHybridDecrypt(handle)
	if err != nil {
		return nil, fmt.Errorf("error creando HybridDecrypt: %w", err)
	}

	// El AAD debe ser exactamente el videoID
	dek, err := hybridDecrypt.Decrypt(wrappedDEK, []byte(videoID))
	if err != nil {
		return nil, fmt.Errorf("fallo al descifrar la DEK (posible manipulación): %w", err)
	}

	return dek, nil
}

// GetDecryptingReader envuelve un lector cifrado y lo convierte en uno descifrado al vuelo.
func GetDecryptingReader(encryptedReader io.Reader, dek []byte, videoID string, isVideo bool) (io.Reader, error) {
	aead, err := subtle.NewAESGCMHKDF(
		dek,
		"SHA256",
		32,
		1<<20, // 1 MiB segment size
		0,
	)
	if err != nil {
		return nil, fmt.Errorf("error inicializando Streaming AEAD: %w", err)
	}

	var aad []byte
	if isVideo {
		aad = []byte(videoID + ":video")
	} else {
		aad = []byte(videoID + ":audio")
	}

	return aead.NewDecryptingReader(encryptedReader, aad)
}
