package main

import (
	"bufio"
	"fmt"
	"net/url"
	"os"
	"strings"
)

// devEnvLocations son las rutas del .env de desarrollo, igual que en el Core: se prueban dos
// porque el IDE puede arrancar desde la raíz del repo o desde el módulo. En Docker no existen y
// las variables llegan del docker-compose.
var devEnvLocations = []string{"../../deploy/.env", "deploy/.env"}

// LoadDevEnv carga deploy/.env si existe, sin sobrescribir nunca una variable ya definida en el
// entorno real (así el IDE o Docker siempre mandan). Después deriva DATABASE_URL y MINIO_ENDPOINT
// de las variables POSTGRES_* y MINIO_* que ya usa el resto del proyecto.
// Devuelve la ruta cargada, o "" si no había ninguna. Nunca registra valores: son secretos.
func LoadDevEnv() (string, error) {
	for _, path := range devEnvLocations {
		file, err := os.Open(path)
		if err != nil {
			continue
		}
		defer file.Close()
		if err := applyEnv(file, path); err != nil {
			return path, err
		}
		deriveConnectionVars()
		return path, nil
	}
	return "", nil
}

func applyEnv(file *os.File, path string) error {
	scanner := bufio.NewScanner(file)
	for line := 1; scanner.Scan(); line++ {
		text := strings.TrimSpace(scanner.Text())
		if text == "" || strings.HasPrefix(text, "#") {
			continue
		}
		key, value, ok := strings.Cut(text, "=")
		if !ok {
			return fmt.Errorf("%s:%d: se esperaba CLAVE=valor", path, line)
		}
		key = strings.TrimSpace(key)
		value = strings.Trim(strings.TrimSpace(value), `"'`)
		if _, exists := os.LookupEnv(key); !exists {
			os.Setenv(key, value)
		}
	}
	return scanner.Err()
}

// deriveConnectionVars arma las variables que espera main.go a partir de las del .env común.
// La contraseña se codifica en la URL para que caracteres como @ o / no la rompan.
func deriveConnectionVars() {
	if os.Getenv("DATABASE_URL") == "" && os.Getenv("POSTGRES_USER") != "" {
		dsn := url.URL{
			Scheme: "postgres",
			User:   url.UserPassword(os.Getenv("POSTGRES_USER"), os.Getenv("POSTGRES_PASSWORD")),
			Host:   "localhost:" + envOr("POSTGRES_PORT", "5432"),
			Path:   "/" + envOr("POSTGRES_DB", "youbeam"),
			// Solo para desarrollo: PostgreSQL corre en la misma máquina, sin TLS
			RawQuery: "sslmode=disable",
		}
		os.Setenv("DATABASE_URL", dsn.String())
	}
	if os.Getenv("MINIO_ENDPOINT") == "" && os.Getenv("MINIO_USER") != "" {
		os.Setenv("MINIO_ENDPOINT", "localhost:"+envOr("MINIO_API_PORT", "9000"))
	}
}

func envOr(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
