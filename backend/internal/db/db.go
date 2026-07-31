package db

import (
	"context"
	"database/sql"
	"embed"
	"fmt"
	"log"

	_ "modernc.org/sqlite"
)

//go:embed migrations/*.sql
var migrations embed.FS

func Open(dbPath string) (*sql.DB, error) {
	db, err := sql.Open("sqlite", dbPath+"?_pragma=journal_mode(WAL)&_pragma=busy_timeout(5000)&_pragma=foreign_keys(1)")
	if err != nil {
		return nil, fmt.Errorf("db open: %w", err)
	}

	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("db ping: %w", err)
	}

	if err := migrate(db); err != nil {
		return nil, fmt.Errorf("db migrate: %w", err)
	}

	if err := ensureDayOwnedBlocks(db); err != nil {
		return nil, fmt.Errorf("db migrate day-owned blocks: %w", err)
	}

	log.Println("db: connected and migrated")
	return db, nil
}

func migrate(db *sql.DB) error {
	entries, err := migrations.ReadDir("migrations")
	if err != nil {
		return fmt.Errorf("read migrations dir: %w", err)
	}

	for _, e := range entries {
		if e.IsDir() {
			continue
		}
		sqlBytes, err := migrations.ReadFile("migrations/" + e.Name())
		if err != nil {
			return fmt.Errorf("read migration %s: %w", e.Name(), err)
		}
		if _, err := db.Exec(string(sqlBytes)); err != nil {
			return fmt.Errorf("exec migration %s: %w", e.Name(), err)
		}
		log.Printf("db: applied migration %s", e.Name())
	}

	return nil
}

// ensureDayOwnedBlocks upgrades time_blocks so that a special day can own its own
// snapshot of blocks (copy-on-write). Existing databases still have the old
// NOT NULL template_id schema; rebuild the table idempotently, guarded by column
// inspection rather than a version number (migrations rerun on every start).
func ensureDayOwnedBlocks(db *sql.DB) error {
	cols, err := db.Query(`SELECT name FROM pragma_table_info('time_blocks')`)
	if err != nil {
		return fmt.Errorf("inspect time_blocks: %w", err)
	}
	hasEntryID := false
	for cols.Next() {
		var name string
		if err := cols.Scan(&name); err != nil {
			cols.Close()
			return fmt.Errorf("scan time_blocks columns: %w", err)
		}
		if name == "entry_id" {
			hasEntryID = true
		}
	}
	cols.Close()
	if hasEntryID {
		// Index may be missing on databases migrated before the rebuild path existed.
		if _, err := db.Exec(`CREATE INDEX IF NOT EXISTS idx_time_blocks_entry ON time_blocks(entry_id)`); err != nil {
			return fmt.Errorf("ensure entry index: %w", err)
		}
		return nil
	}

	log.Println("db: migrating time_blocks to day-owned schema")
	conn, err := db.Conn(context.Background())
	if err != nil {
		return fmt.Errorf("acquire conn: %w", err)
	}
	defer conn.Close()

	// foreign_keys is a per-connection pragma and a no-op inside a transaction;
	// toggle it on this dedicated connection around the rebuild.
	if _, err := conn.ExecContext(context.Background(), `PRAGMA foreign_keys=OFF`); err != nil {
		return fmt.Errorf("disable fk: %w", err)
	}
	defer func() {
		_, _ = conn.ExecContext(context.Background(), `PRAGMA foreign_keys=ON`)
	}()

	const rebuild = `
BEGIN;
CREATE TABLE time_blocks_new (
    id TEXT PRIMARY KEY,
    template_id TEXT REFERENCES day_templates(id) ON DELETE CASCADE,
    entry_id TEXT REFERENCES schedule_entries(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    icon TEXT NOT NULL DEFAULT '',
    mode TEXT NOT NULL CHECK(mode IN ('start_end','start_duration')),
    start_time TEXT NOT NULL,
    end_time TEXT,
    duration_min INTEGER,
    block_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    CHECK ((template_id IS NULL) != (entry_id IS NULL))
);
INSERT INTO time_blocks_new (id, template_id, entry_id, name, icon, mode, start_time, end_time, duration_min, block_order, created_at)
    SELECT id, template_id, NULL, name, icon, mode, start_time, end_time, duration_min, block_order, created_at
    FROM time_blocks;
DROP TABLE time_blocks;
ALTER TABLE time_blocks_new RENAME TO time_blocks;
CREATE INDEX idx_time_blocks_template ON time_blocks(template_id);
CREATE INDEX idx_time_blocks_entry ON time_blocks(entry_id);
COMMIT;`
	if _, err := conn.ExecContext(context.Background(), rebuild); err != nil {
		return fmt.Errorf("rebuild time_blocks: %w", err)
	}
	log.Println("db: time_blocks migrated (day-owned blocks enabled)")
	return nil
}
