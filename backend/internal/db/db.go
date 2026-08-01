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
// snapshot of blocks (copy-on-write), and migrates the old single-owner
// template_id schema to the many-to-many library model:
//   - template_blocks junction always exists (blocks belong to 0..N templates)
//   - legacy template_id rows are backfilled into the junction (idempotent)
//   - time_blocks is rebuilt without template_id, and with entry_id for
//     day-owned snapshot rows when it is missing
//
// Guarded by column inspection rather than a version number (migrations rerun
// on every start).
func ensureDayOwnedBlocks(db *sql.DB) error {
	// Junction must exist even on databases migrated before it was introduced.
	if _, err := db.Exec(`CREATE TABLE IF NOT EXISTS template_blocks (
		template_id TEXT NOT NULL REFERENCES day_templates(id) ON DELETE CASCADE,
		block_id TEXT NOT NULL REFERENCES time_blocks(id) ON DELETE CASCADE,
		block_order INTEGER NOT NULL DEFAULT 0,
		PRIMARY KEY (template_id, block_id)
	)`); err != nil {
		return fmt.Errorf("ensure template_blocks: %w", err)
	}

	cols, err := db.Query(`SELECT name FROM pragma_table_info('time_blocks')`)
	if err != nil {
		return fmt.Errorf("inspect time_blocks: %w", err)
	}
	hasEntryID, hasTemplateID := false, false
	for cols.Next() {
		var name string
		if err := cols.Scan(&name); err != nil {
			cols.Close()
			return fmt.Errorf("scan time_blocks columns: %w", err)
		}
		if name == "entry_id" {
			hasEntryID = true
		}
		if name == "template_id" {
			hasTemplateID = true
		}
	}
	cols.Close()

	if !hasTemplateID {
		// Fresh schema (001) or already migrated: nothing to rebuild.
		if _, err := db.Exec(`CREATE INDEX IF NOT EXISTS idx_time_blocks_entry ON time_blocks(entry_id)`); err != nil {
			return fmt.Errorf("ensure entry index: %w", err)
		}
		return nil
	}

	// Legacy database: backfill the junction, then rebuild without template_id.
	if _, err := db.Exec(`
		INSERT OR IGNORE INTO template_blocks (template_id, block_id, block_order)
		SELECT template_id, id, block_order FROM time_blocks WHERE template_id IS NOT NULL
	`); err != nil {
		return fmt.Errorf("backfill template_blocks: %w", err)
	}
	log.Println("db: migrating time_blocks to library + day-owned schema")

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

	const newTable = `CREATE TABLE time_blocks_new (
		id TEXT PRIMARY KEY,
		entry_id TEXT REFERENCES schedule_entries(id) ON DELETE CASCADE,
		name TEXT NOT NULL,
		icon TEXT NOT NULL DEFAULT '',
		mode TEXT NOT NULL CHECK(mode IN ('start_end','start_duration')),
		start_time TEXT NOT NULL,
		end_time TEXT,
		duration_min INTEGER,
		block_order INTEGER NOT NULL DEFAULT 0,
		created_at TEXT NOT NULL DEFAULT (datetime('now'))
	)`
	copyCols := "name, icon, mode, start_time, end_time, duration_min, block_order, created_at"
	if hasEntryID {
		// entry_id exists: carry it over and just drop template_id.
		rebuild := `BEGIN;` + newTable + `;
INSERT INTO time_blocks_new (id, entry_id, ` + copyCols + `)
	SELECT id, entry_id, ` + copyCols + ` FROM time_blocks;
DROP TABLE time_blocks;
ALTER TABLE time_blocks_new RENAME TO time_blocks;
CREATE INDEX idx_time_blocks_entry ON time_blocks(entry_id);
COMMIT;`
		if _, err := conn.ExecContext(context.Background(), rebuild); err != nil {
			return fmt.Errorf("rebuild time_blocks: %w", err)
		}
	} else {
		// Pre-entry_id legacy: the rebuild introduces day-owned blocks.
		rebuild := `BEGIN;` + newTable + `;
INSERT INTO time_blocks_new (id, ` + copyCols + `)
	SELECT id, ` + copyCols + ` FROM time_blocks;
DROP TABLE time_blocks;
ALTER TABLE time_blocks_new RENAME TO time_blocks;
CREATE INDEX idx_time_blocks_entry ON time_blocks(entry_id);
COMMIT;`
		if _, err := conn.ExecContext(context.Background(), rebuild); err != nil {
			return fmt.Errorf("rebuild time_blocks: %w", err)
		}
	}
	log.Println("db: time_blocks migrated (library + day-owned blocks enabled)")
	return nil
}
