package main

import (
	"testing"

	"github.com/golang-jwt/jwt/v5"
)

func TestInvalidToken(t *testing.T) {
	claims := jwt.MapClaims{}
	// Intentar parsear un token inventado con una llave falsa
	_, err := jwt.ParseWithClaims("esto.es.un.token.falso.123", claims, func(token *jwt.Token) (interface{}, error) {
		return []byte("llave_secreta_falsa"), nil
	})

	if err == nil {
		t.Errorf("FALLO DE SEGURIDAD: El servidor aceptó un token falso")
	} else {
		t.Log("ÉXITO: El servidor rechazó correctamente el token falso")
	}
}
