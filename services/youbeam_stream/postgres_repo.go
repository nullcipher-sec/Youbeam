package main

import (
	"database/sql"
	"fmt"
	"time"
)

type PostgresRepository struct {
	DB *sql.DB
}

func (p *PostgresRepository) SaveUsedTicket(jti string, exp time.Time) error {
	// ON CONFLICT DO NOTHING evita caídas si dos hilos intentan el mismo ticket
	res, err := p.DB.Exec("INSERT INTO used_play_tickets (jti, expires_at) VALUES ($1, $2) ON CONFLICT (jti) DO NOTHING", jti, exp)
	if err != nil {
		return err
	}
	rows, _ := res.RowsAffected()
	if rows == 0 {
		return fmt.Errorf("ticket ya utilizado (replay detectado)")
	}
	return nil
}

func (p *PostgresRepository) GetWrappedDEK(videoID string) ([]byte, error) {
	var wrappedDEK []byte
	// Lee la tabla exacta que creó Marcelo/Jhonmar
	err := p.DB.QueryRow("SELECT wrapped_dek FROM video_keys WHERE video_id = $1", videoID).Scan(&wrappedDEK)
	return wrappedDEK, err
}
