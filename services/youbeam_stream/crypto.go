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

// UnsealDEK descifra la llave (DEK) usando HPKE y la llave privada de Docker Secrets.
func UnsealDEK(wrappedDEK []byte, videoID string) ([]byte, error) {
	privKeyJSON, err := os.ReadFile("hpke_private.json")
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
		return nil, fmt.Errorf("fallo al descifrar la DEK: %w", err)
	}

	return dek, nil
}

// GetDecryptingReader envuelve un lector cifrado y lo convierte en uno descifrado al vuelo.
func GetDecryptingReader(encryptedReader io.Reader, dek []byte, videoID string, isVideo bool) (io.Reader, error) {
	// Usamos NewAESGCMHKDF del paquete subtle.
	// Parámetros: mainKey, hkdfAlg, keySizeInBytes, segmentSize, firstSegmentOffset
	aead, err := subtle.NewAESGCMHKDF(
		dek,      // DEK de 32 bytes para AES-256
		"SHA256", // Algoritmo HKDF
		32,       // Tamaño de la clave en bytes (32 = AES-256)
		1<<20,    // Tamaño del segmento: 1 MiB
		0,        // Offset del primer segmento
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
