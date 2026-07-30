package api

import (
	"context"
	"database/sql"
	"log"
	"time"
)

// StartAutoStatusUpdater periodically updates auto_status for schedule blocks.
// Runs every 30 seconds and transitions blocks based on current time.
func StartAutoStatusUpdater(ctx context.Context, db *sql.DB) {
	go func() {
		ticker := time.NewTicker(30 * time.Second)
		defer ticker.Stop()

		for {
			select {
			case <-ctx.Done():
				log.Println("auto-status updater stopped")
				return
			case <-ticker.C:
				if err := updateAutoStatuses(db); err != nil {
					log.Printf("auto-status update error: %v", err)
				}
			}
		}
	}()
	log.Println("auto-status updater started")
}

func updateAutoStatuses(db *sql.DB) error {
	now := time.Now()
	today := now.Format("2006-01-02")
	currentTime := now.Format("15:04")

	// Find blocks that should be in_progress (start_time <= now < end_time/duration)
	_, err := db.Exec(`
		UPDATE time_block_states SET auto_status = 'in_progress'
		WHERE entry_id IN (SELECT id FROM schedule_entries WHERE date = ?)
		AND auto_status = 'pending'
		AND time_block_id IN (
			SELECT id FROM time_blocks
			WHERE start_time <= ?
			AND (
				(mode = 'start_end' AND end_time > ?)
				OR (mode = 'start_duration' AND time(time(start_time), '+' || duration_min || ' minutes') > ?)
			)
		)
	`, today, currentTime, currentTime, currentTime)
	if err != nil {
		return err
	}

	// Find blocks that should be completed (past end time)
	_, err = db.Exec(`
		UPDATE time_block_states SET auto_status = 'completed'
		WHERE entry_id IN (SELECT id FROM schedule_entries WHERE date = ?)
		AND auto_status = 'in_progress'
		AND time_block_id IN (
			SELECT id FROM time_blocks
			WHERE (mode = 'start_end' AND end_time <= ?)
			OR (mode = 'start_duration' AND time(time(start_time), '+' || duration_min || ' minutes') <= ?)
		)
	`, today, currentTime, currentTime)

	return err
}
