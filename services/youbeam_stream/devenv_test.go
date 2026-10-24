package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadDevEnvDerivesConnectionVarsWithoutOverriding(t *testing.T) {
	dir := t.TempDir()
	envFile := filepath.Join(dir, ".env")
	content := "# comentario\nPOSTGRES_USER=youbeam\nPOSTGRES_PASSWORD=p@ss/word\nPOSTGRES_DB=youbeam\n" +
		"MINIO_USER=minio\nMINIO_PASSWORD='secret'\nMINIO_API_PORT=9000\n"
	if err := os.WriteFile(envFile, []byte(content), 0o600); err != nil {
		t.Fatal(err)
	}
	for _, key := range []string{"POSTGRES_USER", "POSTGRES_PASSWORD", "POSTGRES_DB", "POSTGRES_PORT",
		"MINIO_PASSWORD", "MINIO_API_PORT", "DATABASE_URL", "MINIO_ENDPOINT"} {
		t.Setenv(key, "")
		os.Unsetenv(key)
	}
	// Una variable ya definida en el entorno real no se sobrescribe
	t.Setenv("MINIO_USER", "from-ide")

	original := devEnvLocations
	devEnvLocations = []string{filepath.Join(dir, "missing"), envFile}
	defer func() { devEnvLocations = original }()

	path, err := LoadDevEnv()
	if err != nil || path != envFile {
		t.Fatalf("LoadDevEnv() = %q, %v", path, err)
	}
	if got := os.Getenv("MINIO_USER"); got != "from-ide" {
		t.Errorf("MINIO_USER = %q, no debe sobrescribirse", got)
	}
	if got := os.Getenv("MINIO_PASSWORD"); got != "secret" {
		t.Errorf("MINIO_PASSWORD = %q, deben quitarse las comillas", got)
	}
	if got, want := os.Getenv("DATABASE_URL"), "postgres://youbeam:p%40ss%2Fword@localhost:5432/youbeam?sslmode=disable"; got != want {
		t.Errorf("DATABASE_URL = %q, want %q", got, want)
	}
	if got := os.Getenv("MINIO_ENDPOINT"); got != "localhost:9000" {
		t.Errorf("MINIO_ENDPOINT = %q", got)
	}
}

func TestLoadDevEnvWithoutFileIsNoOp(t *testing.T) {
	original := devEnvLocations
	devEnvLocations = []string{filepath.Join(t.TempDir(), "missing")}
	defer func() { devEnvLocations = original }()

	if path, err := LoadDevEnv(); path != "" || err != nil {
		t.Fatalf("LoadDevEnv() = %q, %v; se esperaba no hacer nada", path, err)
	}
}
