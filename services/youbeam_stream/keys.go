package main

import (
	"crypto/ecdsa"
	"fmt"
	"os"

	"github.com/golang-jwt/jwt/v5"
)

// ticketKeyLocations son las rutas donde se busca la clave pública de tickets, en orden:
// variable de entorno, Docker secret (con y sin extensión) y deploy/secrets para desarrollo local.
func ticketKeyLocations() []string {
	locations := []string{}
	if path := os.Getenv("JWT_PUBLIC_KEY_PATH"); path != "" {
		locations = append(locations, path)
	}
	return append(locations,
		"/run/secrets/jwt_public",
		"/run/secrets/jwt_public.pem",
		"../../deploy/secrets/jwt_public.pem",
		"deploy/secrets/jwt_public.pem",
	)
}

// LoadTicketPublicKey carga la clave pública ECDSA P-256 con la que se verifican los tickets.
// Si no existe, el servidor no debe arrancar: sin ella no puede verificar nada, y generar una
// clave aleatoria haría que todos los tickets legítimos del Core fueran rechazados.
func LoadTicketPublicKey() (*ecdsa.PublicKey, string, error) {
	for _, path := range ticketKeyLocations() {
		pemBytes, err := os.ReadFile(path)
		if err != nil {
			continue
		}
		key, err := jwt.ParseECPublicKeyFromPEM(pemBytes)
		if err != nil {
			return nil, path, fmt.Errorf("clave pública inválida en %s: %w", path, err)
		}
		if key.Curve.Params().Name != "P-256" {
			return nil, path, fmt.Errorf("la clave de %s debe ser P-256 (ES256)", path)
		}
		return key, path, nil
	}
	return nil, "", fmt.Errorf("no se encontró jwt_public.pem en %v", ticketKeyLocations())
}
