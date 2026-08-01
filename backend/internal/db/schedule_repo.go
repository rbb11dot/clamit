package db

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/yourusername/clamit/internal/models"
)

type ScheduleRepo struct {
	db *sql.DB
}

func NewScheduleRepo(db *sql.DB) *ScheduleRepo {
	return &ScheduleRepo{db: db}
}

// ---- Templates ----

func (r *ScheduleRepo) CreateTemplate(ctx context.Context, req models.CreateTemplateReq) (*models.DayTemplate, error) {
	id := uuid.New().String()
	daysJSON, _ := json.Marshal(req.RepeatDays)

	_, err := r.db.ExecContext(ctx,
		`INSERT INTO day_templates (id, name, icon, repeat_days) VALUES (?, ?, ?, ?)`,
		id, req.Name, req.Icon, string(daysJSON))
	if err != nil {
		return nil, fmt.Errorf("create template: %w", err)
	}

	return r.GetTemplate(ctx, id)
}

func (r *ScheduleRepo) GetTemplate(ctx context.Context, id string) (*models.DayTemplate, error) {
	var t models.DayTemplate
	var daysJSON string
	err := r.db.QueryRowContext(ctx,
		`SELECT id, name, icon, repeat_days, created_at FROM day_templates WHERE id = ?`, id).
		Scan(&t.ID, &t.Name, &t.Icon, &daysJSON, &t.CreatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("get template: %w", err)
	}
	json.Unmarshal([]byte(daysJSON), &t.RepeatDays)
	blocks, err := r.ListTemplateBlocks(ctx, t.ID)
	if err != nil {
		return nil, fmt.Errorf("get template blocks: %w", err)
	}
	t.Blocks = blocks
	return &t, nil
}

func (r *ScheduleRepo) ListTemplates(ctx context.Context) ([]models.DayTemplate, error) {
	rows, err := r.db.QueryContext(ctx,
		`SELECT id, name, icon, repeat_days, created_at FROM day_templates ORDER BY created_at`)
	if err != nil {
		return nil, fmt.Errorf("list templates: %w", err)
	}
	defer rows.Close()

	var templates []models.DayTemplate
	for rows.Next() {
		var t models.DayTemplate
		var daysJSON string
		if err := rows.Scan(&t.ID, &t.Name, &t.Icon, &daysJSON, &t.CreatedAt); err != nil {
			return nil, fmt.Errorf("scan template: %w", err)
		}
		json.Unmarshal([]byte(daysJSON), &t.RepeatDays)
		// The app renders the block library from template lists (Zaman Blokları
		// page, template cards, template picker), so every template ships with
		// its blocks and subtasks. Localhost scale makes the N+1 irrelevant.
		blocks, err := r.ListTemplateBlocks(ctx, t.ID)
		if err != nil {
			return nil, fmt.Errorf("list template blocks: %w", err)
		}
		t.Blocks = blocks
		templates = append(templates, t)
	}
	return templates, nil
}

func (r *ScheduleRepo) UpdateTemplate(ctx context.Context, id string, req models.UpdateTemplateReq) (*models.DayTemplate, error) {
	t, err := r.GetTemplate(ctx, id)
	if err != nil {
		return nil, err
	}
	if t == nil {
		return nil, nil
	}

	if req.Name != nil {
		t.Name = *req.Name
	}
	if req.Icon != nil {
		t.Icon = *req.Icon
	}
	if req.RepeatDays != nil {
		t.RepeatDays = *req.RepeatDays
	}

	daysJSON, _ := json.Marshal(t.RepeatDays)
	_, err = r.db.ExecContext(ctx,
		`UPDATE day_templates SET name=?, icon=?, repeat_days=? WHERE id=?`,
		t.Name, t.Icon, string(daysJSON), id)
	if err != nil {
		return nil, fmt.Errorf("update template: %w", err)
	}

	return r.GetTemplate(ctx, id)
}

func (r *ScheduleRepo) DeleteTemplate(ctx context.Context, id string) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("delete template: %w", err)
	}
	defer tx.Rollback()

	// Copy-on-write: every day using this template becomes a standalone special day
	// holding its own snapshot of the template's blocks, so the deletion never
	// destroys day data.
	rows, err := tx.QueryContext(ctx, `SELECT id FROM schedule_entries WHERE template_id = ?`, id)
	if err != nil {
		return fmt.Errorf("delete template: list entries: %w", err)
	}
	var entryIDs []string
	for rows.Next() {
		var eid string
		if err := rows.Scan(&eid); err != nil {
			rows.Close()
			return fmt.Errorf("delete template: scan entry: %w", err)
		}
		entryIDs = append(entryIDs, eid)
	}
	rows.Close()
	for _, eid := range entryIDs {
		if _, _, err := r.detachEntryFromTemplateTx(ctx, tx, eid, id); err != nil {
			return fmt.Errorf("delete template: detach entry %s: %w", eid, err)
		}
	}

	if _, err = tx.ExecContext(ctx,
		`DELETE FROM day_templates WHERE id=?`, id); err != nil {
		return fmt.Errorf("delete template: %w", err)
	}

	return tx.Commit()
}

// ---- Time Blocks ----

// CreateBlock adds a standalone library block. Blocks are not owned by a
// template; templates reference them through template_blocks (many-to-many).
func (r *ScheduleRepo) CreateBlock(ctx context.Context, req models.CreateBlockReq) (*models.TimeBlock, error) {
	id := uuid.New().String()

	_, err := r.db.ExecContext(ctx,
		`INSERT INTO time_blocks (id, name, icon, mode, start_time, end_time, duration_min, block_order)
		 VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT COALESCE(MAX(block_order), -1) + 1 FROM time_blocks WHERE entry_id IS NULL))`,
		id, req.Name, req.Icon, req.Mode, req.StartTime, req.EndTime, req.DurationMin)
	if err != nil {
		return nil, fmt.Errorf("create block: %w", err)
	}

	// Create subtasks
	for i, s := range req.Subtasks {
		sid := uuid.New().String()
		_, err := r.db.ExecContext(ctx,
			`INSERT INTO subtasks (id, time_block_id, name, subtask_order) VALUES (?, ?, ?, ?)`,
			sid, id, s.Name, i)
		if err != nil {
			return nil, fmt.Errorf("create subtask: %w", err)
		}
	}

	return r.GetBlock(ctx, id)
}

func (r *ScheduleRepo) GetBlock(ctx context.Context, id string) (*models.TimeBlock, error) {
	var b models.TimeBlock
	err := r.db.QueryRowContext(ctx,
		`SELECT id, name, icon, mode, start_time, end_time, duration_min, block_order, created_at
		 FROM time_blocks WHERE id = ?`, id).
		Scan(&b.ID, &b.Name, &b.Icon, &b.Mode, &b.StartTime, &b.EndTime, &b.DurationMin, &b.BlockOrder, &b.CreatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("get block: %w", err)
	}

	subtasks, err := r.ListSubtasks(ctx, id)
	if err != nil {
		return nil, err
	}
	b.Subtasks = subtasks
	return &b, nil
}

// ListTemplateBlocks returns the blocks attached to a template, in junction order.
func (r *ScheduleRepo) ListTemplateBlocks(ctx context.Context, templateID string) ([]models.TimeBlock, error) {
	rows, err := r.db.QueryContext(ctx,
		`SELECT b.id, b.name, b.icon, b.mode, b.start_time, b.end_time, b.duration_min, tb.block_order, b.created_at
		 FROM template_blocks tb
		 JOIN time_blocks b ON b.id = tb.block_id
		 WHERE tb.template_id = ?
		 ORDER BY tb.block_order`, templateID)
	if err != nil {
		return nil, fmt.Errorf("list template blocks: %w", err)
	}
	defer rows.Close()
	return r.scanBlocks(ctx, rows)
}

// ListLibraryBlocks returns every non-day-owned block (the block library).
func (r *ScheduleRepo) ListLibraryBlocks(ctx context.Context) ([]models.TimeBlock, error) {
	rows, err := r.db.QueryContext(ctx,
		`SELECT id, name, icon, mode, start_time, end_time, duration_min, block_order, created_at
		 FROM time_blocks WHERE entry_id IS NULL ORDER BY block_order, created_at`)
	if err != nil {
		return nil, fmt.Errorf("list library blocks: %w", err)
	}
	defer rows.Close()
	return r.scanBlocks(ctx, rows)
}

// scanBlocks scans block rows (including their subtasks) from an open cursor.
func (r *ScheduleRepo) scanBlocks(ctx context.Context, rows *sql.Rows) ([]models.TimeBlock, error) {
	blocks := make([]models.TimeBlock, 0)
	for rows.Next() {
		var b models.TimeBlock
		if err := rows.Scan(&b.ID, &b.Name, &b.Icon, &b.Mode, &b.StartTime, &b.EndTime, &b.DurationMin, &b.BlockOrder, &b.CreatedAt); err != nil {
			return nil, fmt.Errorf("scan block: %w", err)
		}
		subtasks, err := r.ListSubtasks(ctx, b.ID)
		if err != nil {
			return nil, fmt.Errorf("list block subtasks: %w", err)
		}
		b.Subtasks = subtasks
		blocks = append(blocks, b)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("scan blocks: %w", err)
	}
	return blocks, nil
}

// SaveBlock applies a full update to a library block: fields, mode switch
// consistency (start_end clears duration, start_duration clears end time) and
// a subtask sync (create/update/delete/reorder in one transaction).
func (r *ScheduleRepo) SaveBlock(ctx context.Context, blockID string, req models.UpdateBlockReq) (*models.TimeBlock, error) {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, fmt.Errorf("save block: %w", err)
	}
	defer tx.Rollback()

	if err := r.saveBlockTx(ctx, tx, blockID, req, nil); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("save block: commit: %w", err)
	}
	return r.GetBlock(ctx, blockID)
}

// saveBlockTx updates a block inside a transaction. idRemap maps old subtask
// ids to their day-owned copies (used by SaveEntryBlock after a copy-on-write
// detach) so that renames still hit the copy that carries the day's toggles.
func (r *ScheduleRepo) saveBlockTx(ctx context.Context, tx *sql.Tx, blockID string, req models.UpdateBlockReq, idRemap map[string]string) error {
	var name, icon, mode, startTime string
	var endTime sql.NullString
	var duration sql.NullInt64
	err := tx.QueryRowContext(ctx,
		`SELECT name, icon, mode, start_time, end_time, duration_min FROM time_blocks WHERE id = ?`, blockID).
		Scan(&name, &icon, &mode, &startTime, &endTime, &duration)
	if err == sql.ErrNoRows {
		return fmt.Errorf("save block: not found")
	}
	if err != nil {
		return fmt.Errorf("save block: read block: %w", err)
	}

	if req.Name != nil {
		name = *req.Name
	}
	if req.Icon != nil {
		icon = *req.Icon
	}
	if req.StartTime != nil {
		startTime = *req.StartTime
	}

	// Mode switch: keep the block's time representation consistent. Switching to
	// start_duration drops the end time; switching to start_end drops the duration.
	if req.Mode != nil {
		mode = *req.Mode
		if mode == "start_duration" {
			endTime = sql.NullString{}
		} else if mode == "start_end" {
			duration = sql.NullInt64{}
		}
	}
	if req.EndTime != nil {
		if mode == "start_end" {
			endTime = sql.NullString{String: *req.EndTime, Valid: true}
		}
	} else if mode == "start_end" && endTime.Valid {
		// keep existing end time
	}
	if req.DurationMin != nil {
		if mode == "start_duration" {
			duration = sql.NullInt64{Int64: int64(*req.DurationMin), Valid: true}
		}
	}

	if _, err := tx.ExecContext(ctx,
		`UPDATE time_blocks SET name=?, icon=?, mode=?, start_time=?, end_time=?, duration_min=? WHERE id=?`,
		name, icon, mode, startTime, endTime, duration, blockID); err != nil {
		return fmt.Errorf("save block: update: %w", err)
	}

	if req.Subtasks == nil {
		return nil
	}

	// Full subtask sync: list existing, then create/update per the request order,
	// delete anything not listed.
	rows, err := tx.QueryContext(ctx,
		`SELECT id FROM subtasks WHERE time_block_id = ?`, blockID)
	if err != nil {
		return fmt.Errorf("save block: list subtasks: %w", err)
	}
	existing := map[string]bool{}
	for rows.Next() {
		var sid string
		if err := rows.Scan(&sid); err != nil {
			rows.Close()
			return fmt.Errorf("save block: scan subtask: %w", err)
		}
		existing[sid] = true
	}
	rows.Close()

	for i, s := range req.Subtasks {
		sid := s.ID
		if sid != nil && idRemap != nil {
			if mapped, ok := idRemap[*sid]; ok {
				sid = &mapped
			}
		}
		if sid != nil && existing[*sid] {
			if _, err := tx.ExecContext(ctx,
				`UPDATE subtasks SET name=?, subtask_order=? WHERE id=? AND time_block_id=?`,
				s.Name, i, *sid, blockID); err != nil {
				return fmt.Errorf("save block: update subtask: %w", err)
			}
		} else {
			newID := uuid.New().String()
			if _, err := tx.ExecContext(ctx,
				`INSERT INTO subtasks (id, time_block_id, name, subtask_order) VALUES (?, ?, ?, ?)`,
				newID, blockID, s.Name, i); err != nil {
				return fmt.Errorf("save block: create subtask: %w", err)
			}
		}
	}

	// Delete subtasks that were removed. Only ids we did not touch this round.
	kept := map[string]bool{}
	for _, s := range req.Subtasks {
		if s.ID == nil {
			continue
		}
		sid := *s.ID
		if idRemap != nil {
			if mapped, ok := idRemap[sid]; ok {
				sid = mapped
			}
		}
		kept[sid] = true
	}
	for sid := range existing {
		if !kept[sid] {
			if _, err := tx.ExecContext(ctx, `DELETE FROM subtasks WHERE id = ?`, sid); err != nil {
				return fmt.Errorf("save block: delete subtask: %w", err)
			}
		}
	}
	return nil
}

// SaveEntryBlock edits a block inside a specific day. For a template-linked day
// this first performs copy-on-write (the day becomes a standalone special day),
// then updates the day-owned copy — so editing a day's block never mutates the
// shared template, exactly like adding/removing blocks. Subtask ids in the
// request are remapped from the template's ids to the day's copies, preserving
// the day's toggle states.
func (r *ScheduleRepo) SaveEntryBlock(ctx context.Context, date string, blockID string, req models.UpdateBlockReq) (*models.ScheduleEntry, error) {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return nil, err
	}

	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, fmt.Errorf("save entry block: %w", err)
	}
	defer tx.Rollback()

	targetID := blockID
	var subRemap map[string]string
	if entry.TemplateID != nil {
		blockMap, subMap, err := r.detachEntryFromTemplateTx(ctx, tx, entry.ID, *entry.TemplateID)
		if err != nil {
			return nil, fmt.Errorf("save entry block: detach: %w", err)
		}
		copyID, ok := blockMap[blockID]
		if !ok {
			return nil, fmt.Errorf("save entry block: block %s not in day", blockID)
		}
		targetID = copyID
		subRemap = subMap
	}

	if err := r.saveBlockTx(ctx, tx, targetID, req, subRemap); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("save entry block: commit: %w", err)
	}
	return r.GetEntry(ctx, date)
}

func (r *ScheduleRepo) DeleteBlock(ctx context.Context, id string) error {
	_, err := r.db.ExecContext(ctx, `DELETE FROM time_blocks WHERE id = ?`, id)
	return err
}

// AddTemplateBlock attaches an existing library block to a template (junction).
// Idempotent: attaching a block twice is a no-op.
func (r *ScheduleRepo) AddTemplateBlock(ctx context.Context, templateID string, blockID string) error {
	_, err := r.db.ExecContext(ctx,
		`INSERT INTO template_blocks (template_id, block_id, block_order)
		 VALUES (?, ?, (SELECT COALESCE(MAX(block_order), -1) + 1 FROM template_blocks WHERE template_id = ?))
		 ON CONFLICT(template_id, block_id) DO NOTHING`,
		templateID, blockID, templateID)
	if err != nil {
		return fmt.Errorf("add template block: %w", err)
	}
	return nil
}

// RemoveTemplateBlock detaches a block from a template. The block stays in the
// library and in any other template that references it.
func (r *ScheduleRepo) RemoveTemplateBlock(ctx context.Context, templateID string, blockID string) error {
	_, err := r.db.ExecContext(ctx,
		`DELETE FROM template_blocks WHERE template_id = ? AND block_id = ?`, templateID, blockID)
	if err != nil {
		return fmt.Errorf("remove template block: %w", err)
	}
	return nil
}

// ReorderTemplateBlocks sets the order of a template's blocks from a full id list.
func (r *ScheduleRepo) ReorderTemplateBlocks(ctx context.Context, templateID string, ids []string) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback()

	for i, id := range ids {
		_, err := tx.ExecContext(ctx,
			`UPDATE template_blocks SET block_order=? WHERE template_id=? AND block_id=?`, i, templateID, id)
		if err != nil {
			return fmt.Errorf("reorder block %s: %w", id, err)
		}
	}
	return tx.Commit()
}

// ---- Subtasks ----

func (r *ScheduleRepo) CreateSubtask(ctx context.Context, blockID string, req models.SubtaskReq) (*models.Subtask, error) {
	id := uuid.New().String()
	_, err := r.db.ExecContext(ctx,
		`INSERT INTO subtasks (id, time_block_id, name, subtask_order)
		 VALUES (?, ?, ?, (SELECT COALESCE(MAX(subtask_order), -1) + 1 FROM subtasks WHERE time_block_id = ?))`,
		id, blockID, req.Name, blockID)
	if err != nil {
		return nil, fmt.Errorf("create subtask: %w", err)
	}
	return &models.Subtask{ID: id, TimeBlockID: blockID, Name: req.Name}, nil
}

func (r *ScheduleRepo) ListSubtasks(ctx context.Context, blockID string) ([]models.Subtask, error) {
	rows, err := r.db.QueryContext(ctx,
		`SELECT id, time_block_id, name, subtask_order FROM subtasks WHERE time_block_id = ? ORDER BY subtask_order`, blockID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var subtasks []models.Subtask
	for rows.Next() {
		var s models.Subtask
		if err := rows.Scan(&s.ID, &s.TimeBlockID, &s.Name, &s.SubtaskOrder); err != nil {
			return nil, err
		}
		subtasks = append(subtasks, s)
	}
	return subtasks, nil
}

func (r *ScheduleRepo) UpdateSubtask(ctx context.Context, id string, req models.UpdateSubtaskReq) (*models.Subtask, error) {
	if req.Name != nil {
		_, err := r.db.ExecContext(ctx, `UPDATE subtasks SET name=? WHERE id=?`, *req.Name, id)
		if err != nil {
			return nil, fmt.Errorf("update subtask: %w", err)
		}
	}
	var s models.Subtask
	err := r.db.QueryRowContext(ctx,
		`SELECT id, time_block_id, name, subtask_order FROM subtasks WHERE id = ?`, id).
		Scan(&s.ID, &s.TimeBlockID, &s.Name, &s.SubtaskOrder)
	if err != nil {
		return nil, fmt.Errorf("get subtask: %w", err)
	}
	return &s, nil
}

func (r *ScheduleRepo) DeleteSubtask(ctx context.Context, id string) error {
	_, err := r.db.ExecContext(ctx, `DELETE FROM subtasks WHERE id = ?`, id)
	return err
}

func (r *ScheduleRepo) ReorderSubtasks(ctx context.Context, ids []string) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback()

	for i, id := range ids {
		_, err := tx.ExecContext(ctx, `UPDATE subtasks SET subtask_order=? WHERE id=?`, i, id)
		if err != nil {
			return fmt.Errorf("reorder subtask %s: %w", id, err)
		}
	}
	return tx.Commit()
}

// ---- Schedule Entries ----

func (r *ScheduleRepo) GetOrCreateEntry(ctx context.Context, date string) (*models.ScheduleEntry, error) {
	// Try to get existing
	entry, err := r.GetEntry(ctx, date)
	if err != nil {
		return nil, err
	}
	if entry != nil {
		return entry, nil
	}

	// Find matching template
	tmpl, err := r.findTemplateForDate(ctx, date)
	if err != nil {
		return nil, err
	}

	id := uuid.New().String()
	if tmpl != nil {
		_, err = r.db.ExecContext(ctx,
			`INSERT OR IGNORE INTO schedule_entries (id, date, template_id, is_special) VALUES (?, ?, ?, 0)`,
			id, date, tmpl.ID)
	} else {
		_, err = r.db.ExecContext(ctx,
			`INSERT OR IGNORE INTO schedule_entries (id, date, template_id, is_special) VALUES (?, ?, NULL, 1)`,
			id, date)
	}
	if err != nil {
		return nil, fmt.Errorf("create entry: %w", err)
	}

	// Create block states from template (only if we won the insert)
	if tmpl != nil {
		// Check if we actually inserted (rowcount check is unreliable with INSERT OR IGNORE)
		// Always try to create block states; GetEntry handles dedup via SELECT
		blocks, err := r.ListTemplateBlocks(ctx, tmpl.ID)
		if err != nil {
			return nil, err
		}
		for _, b := range blocks {
			// Skip if state already exists (concurrent winner created it)
			var existing string
			if err := r.db.QueryRowContext(ctx,
				`SELECT id FROM time_block_states WHERE entry_id=? AND time_block_id=?`,
				id, b.ID).Scan(&existing); err == sql.ErrNoRows {
				if err := r.createBlockState(ctx, id, b.ID); err != nil {
					return nil, err
				}
			}
		}
	}

	return r.GetEntry(ctx, date)
}

func (r *ScheduleRepo) GetEntry(ctx context.Context, date string) (*models.ScheduleEntry, error) {
	var e models.ScheduleEntry
	var tmplID sql.NullString
	var tmplName sql.NullString
	err := r.db.QueryRowContext(ctx,
		`SELECT e.id, e.date, e.template_id, e.is_special, e.created_at, dt.name
		 FROM schedule_entries e LEFT JOIN day_templates dt ON e.template_id = dt.id
		 WHERE e.date = ?`, date).
		Scan(&e.ID, &e.Date, &tmplID, &e.IsSpecial, &e.CreatedAt, &tmplName)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("get entry: %w", err)
	}
	if tmplID.Valid {
		e.TemplateID = &tmplID.String
	}
	if tmplName.Valid {
		e.TemplateName = &tmplName.String
	}

	// Load blocks with states
	blocks, err := r.getBlockStates(ctx, e.ID, e.TemplateID)
	if err != nil {
		return nil, err
	}
	e.Blocks = blocks

	return &e, nil
}

func (r *ScheduleRepo) SetEntryTemplate(ctx context.Context, date string, templateID *string) (*models.ScheduleEntry, error) {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return nil, err
	}

	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, fmt.Errorf("set template: %w", err)
	}
	defer tx.Rollback()

	if templateID == nil {
		// Convert to a standalone special day (copy-on-write snapshot).
		if entry.TemplateID != nil {
			if _, _, err := r.detachEntryFromTemplateTx(ctx, tx, entry.ID, *entry.TemplateID); err != nil {
				return nil, err
			}
		} else if !entry.IsSpecial {
			if _, err = tx.ExecContext(ctx,
				`UPDATE schedule_entries SET template_id=NULL, is_special=1 WHERE id=?`, entry.ID); err != nil {
				return nil, fmt.Errorf("set template: mark special: %w", err)
			}
		}
	} else {
		// Attach a template: drop any day-owned snapshot blocks, then link and
		// rebuild the day's states from the template.
		if _, err = tx.ExecContext(ctx,
			`DELETE FROM time_blocks WHERE entry_id = ?`, entry.ID); err != nil {
			return nil, fmt.Errorf("set template: clear day blocks: %w", err)
		}
		if _, err = tx.ExecContext(ctx,
			`UPDATE schedule_entries SET template_id=?, is_special=0 WHERE id=?`, *templateID, entry.ID); err != nil {
			return nil, fmt.Errorf("set template: %w", err)
		}
	}
	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("set template: commit: %w", err)
	}

	if templateID != nil {
		if rebuildErr := r.rebuildBlockStates(ctx, entry.ID, *templateID); rebuildErr != nil {
			return nil, fmt.Errorf("rebuild block states: %w", rebuildErr)
		}
	}

	return r.GetEntry(ctx, date)
}

// ---- Template → special-day adoption ----
// Days that were materialized as special days before their template existed
// silently miss the template. These helpers list such days for a template's
// repeat days and convert them into template-linked days on demand.

// ListSpecialDaysForTemplate returns the ISO dates of special days (no
// template) whose weekday matches the template's repeatDays, ordered by date.
func (r *ScheduleRepo) ListSpecialDaysForTemplate(ctx context.Context, templateID string) ([]string, error) {
	tmpl, err := r.GetTemplate(ctx, templateID)
	if err != nil {
		return nil, err
	}
	if tmpl == nil {
		return nil, nil
	}

	rows, err := r.db.QueryContext(ctx,
		`SELECT date FROM schedule_entries WHERE is_special = 1 AND template_id IS NULL ORDER BY date`)
	if err != nil {
		return nil, fmt.Errorf("list special days: %w", err)
	}
	defer rows.Close()

	repeat := make(map[int]bool, len(tmpl.RepeatDays))
	for _, d := range tmpl.RepeatDays {
		repeat[d] = true
	}

	var dates []string
	for rows.Next() {
		var date string
		if err := rows.Scan(&date); err != nil {
			return nil, fmt.Errorf("list special days: scan: %w", err)
		}
		t, perr := time.Parse("2006-01-02", date)
		if perr != nil {
			continue
		}
		if repeat[int(t.Weekday())] {
			dates = append(dates, date)
		}
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("list special days: %w", err)
	}
	return dates, nil
}

// ApplyTemplateToDates converts the given dates into template-linked days: the
// day's special content is dropped (copy-on-write snapshot discarded) and the
// template's blocks and states are rebuilt. Only days that are currently special
// and whose weekday matches the template's repeatDays are converted; everything
// else is skipped. Returns the number of days converted.
func (r *ScheduleRepo) ApplyTemplateToDates(ctx context.Context, templateID string, dates []string) (int, error) {
	tmpl, err := r.GetTemplate(ctx, templateID)
	if err != nil {
		return 0, err
	}
	if tmpl == nil {
		return 0, nil
	}
	repeat := make(map[int]bool, len(tmpl.RepeatDays))
	for _, d := range tmpl.RepeatDays {
		repeat[d] = true
	}

	applied := 0
	for _, date := range dates {
		t, perr := time.Parse("2006-01-02", date)
		if perr != nil || !repeat[int(t.Weekday())] {
			continue
		}
		entry, err := r.GetEntry(ctx, date)
		if err != nil {
			return applied, err
		}
		if entry == nil || !entry.IsSpecial {
			continue
		}
		if _, err := r.SetEntryTemplate(ctx, date, &templateID); err != nil {
			return applied, fmt.Errorf("apply template to %s: %w", date, err)
		}
		applied++
	}
	return applied, nil
}

func (r *ScheduleRepo) AddSpecialBlock(ctx context.Context, date string, blockID string) error {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return err
	}

	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("add special block: %w", err)
	}
	defer tx.Rollback()

	// Copy-on-write: a template-linked day detaches into its own snapshot first,
	// so adding a block never silently mutates the shared template.
	if entry.TemplateID != nil {
		if _, _, err := r.detachEntryFromTemplateTx(ctx, tx, entry.ID, *entry.TemplateID); err != nil {
			return err
		}
	} else if !entry.IsSpecial {
		if _, err = tx.ExecContext(ctx,
			`UPDATE schedule_entries SET is_special=1 WHERE id=?`, entry.ID); err != nil {
			return fmt.Errorf("add special block: mark special: %w", err)
		}
	}

	// The added block becomes day-owned too, so it survives its library template's
	// deletion. The state row points at the day-owned copy.
	copyID, _, err := r.copyBlockToEntryTx(ctx, tx, entry.ID, blockID)
	if err != nil {
		return err
	}
	if err = r.createBlockStateTx(ctx, tx, entry.ID, copyID); err != nil {
		return err
	}
	return tx.Commit()
}

func (r *ScheduleRepo) RemoveSpecialBlock(ctx context.Context, date string, blockID string) error {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return err
	}

	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("remove special block: %w", err)
	}
	defer tx.Rollback()

	if _, err = tx.ExecContext(ctx,
		`DELETE FROM time_block_states WHERE entry_id=? AND time_block_id=?`, entry.ID, blockID); err != nil {
		return fmt.Errorf("remove special block: %w", err)
	}
	// Drop the day-owned copy (cascades its subtasks).
	if _, err = tx.ExecContext(ctx,
		`DELETE FROM time_blocks WHERE id=? AND entry_id=?`, blockID, entry.ID); err != nil {
		return fmt.Errorf("remove special block: drop copy: %w", err)
	}
	return tx.Commit()
}

// copyBlockToEntryTx copies a library block (and its subtasks) into a day-owned row
// owned by entryID. Returns the new day-owned block id plus a map of old subtask id
// to new subtask id. Runs inside a transaction.
func (r *ScheduleRepo) copyBlockToEntryTx(ctx context.Context, tx *sql.Tx, entryID string, srcBlockID string) (string, map[string]string, error) {
	var b struct {
		name       string
		icon       string
		mode       string
		startTime  string
		endTime    sql.NullString
		duration   sql.NullInt64
		blockOrder int
	}
	err := tx.QueryRowContext(ctx,
		`SELECT name, icon, mode, start_time, end_time, duration_min, block_order
		 FROM time_blocks WHERE id = ?`, srcBlockID).
		Scan(&b.name, &b.icon, &b.mode, &b.startTime, &b.endTime, &b.duration, &b.blockOrder)
	if err != nil {
		return "", nil, fmt.Errorf("copy block: read source: %w", err)
	}

	newID := uuid.New().String()
	_, err = tx.ExecContext(ctx,
		`INSERT INTO time_blocks (id, entry_id, name, icon, mode, start_time, end_time, duration_min, block_order)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
		newID, entryID, b.name, b.icon, b.mode, b.startTime, b.endTime, b.duration, b.blockOrder)
	if err != nil {
		return "", nil, fmt.Errorf("copy block: insert copy: %w", err)
	}

	rows, err := tx.QueryContext(ctx,
		`SELECT id, name, subtask_order FROM subtasks WHERE time_block_id = ? ORDER BY subtask_order`, srcBlockID)
	if err != nil {
		return "", nil, fmt.Errorf("copy block: list subtasks: %w", err)
	}
	defer rows.Close()
	subMap := make(map[string]string)
	for rows.Next() {
		var sid, sname string
		var sorder int
		if err := rows.Scan(&sid, &sname, &sorder); err != nil {
			return "", nil, fmt.Errorf("copy block: scan subtask: %w", err)
		}
		newSubID := uuid.New().String()
		if _, err := tx.ExecContext(ctx,
			`INSERT INTO subtasks (id, time_block_id, name, subtask_order) VALUES (?, ?, ?, ?)`,
			newSubID, newID, sname, sorder); err != nil {
			return "", nil, fmt.Errorf("copy block: insert subtask: %w", err)
		}
		subMap[sid] = newSubID
	}
	if err := rows.Err(); err != nil {
		return "", nil, fmt.Errorf("copy block: subtasks: %w", err)
	}
	return newID, subMap, nil
}

// detachEntryFromTemplateTx implements copy-on-write for a special day: the entry's
// template blocks (and subtasks) are copied into day-owned rows, the day's existing
// state/subtask-state rows are re-pointed at the copies (preserving status and
// toggles), and the entry is marked as a standalone special day. Runs inside a tx.
// Returns old-block-id → day-owned-copy-id and old-subtask-id → copied-subtask-id
// maps so callers can keep addressing the same logical items after the detach.
func (r *ScheduleRepo) detachEntryFromTemplateTx(ctx context.Context, tx *sql.Tx, entryID string, templateID string) (map[string]string, map[string]string, error) {
	rows, err := tx.QueryContext(ctx,
		`SELECT b.id FROM template_blocks tb
		 JOIN time_blocks b ON b.id = tb.block_id
		 WHERE tb.template_id = ? ORDER BY tb.block_order`, templateID)
	if err != nil {
		return nil, nil, fmt.Errorf("detach: list blocks: %w", err)
	}
	type blockCopy struct {
		oldID  string
		newID  string
		subMap map[string]string
	}
	var copies []blockCopy
	for rows.Next() {
		var oldID string
		if err := rows.Scan(&oldID); err != nil {
			rows.Close()
			return nil, nil, fmt.Errorf("detach: scan block: %w", err)
		}
		newID, subMap, err := r.copyBlockToEntryTx(ctx, tx, entryID, oldID)
		if err != nil {
			rows.Close()
			return nil, nil, fmt.Errorf("detach: copy block %s: %w", oldID, err)
		}
		copies = append(copies, blockCopy{oldID: oldID, newID: newID, subMap: subMap})
	}
	rows.Close()
	if err := rows.Err(); err != nil {
		return nil, nil, fmt.Errorf("detach: list blocks: %w", err)
	}

	blockMap := make(map[string]string, len(copies))
	subMap := make(map[string]string)
	// Re-point the day's states at the copies, preserving status and toggles.
	for _, c := range copies {
		blockMap[c.oldID] = c.newID
		for oldSub, newSub := range c.subMap {
			subMap[oldSub] = newSub
		}
		if _, err := tx.ExecContext(ctx,
			`UPDATE time_block_states SET time_block_id = ? WHERE entry_id = ? AND time_block_id = ?`,
			c.newID, entryID, c.oldID); err != nil {
			return nil, nil, fmt.Errorf("detach: re-point states: %w", err)
		}
		for oldSub, newSub := range c.subMap {
			if _, err := tx.ExecContext(ctx,
				`UPDATE subtask_states SET subtask_id = ?
				 WHERE time_block_state_id IN (SELECT id FROM time_block_states WHERE entry_id = ? AND time_block_id = ?)
				   AND subtask_id = ?`,
				newSub, entryID, c.newID, oldSub); err != nil {
				return nil, nil, fmt.Errorf("detach: re-point subtask states: %w", err)
			}
		}
	}

	if _, err := tx.ExecContext(ctx,
		`UPDATE schedule_entries SET template_id = NULL, is_special = 1 WHERE id = ?`, entryID); err != nil {
		return nil, nil, fmt.Errorf("detach: mark special: %w", err)
	}
	return blockMap, subMap, nil
}

// ---- Status Updates ----

func (r *ScheduleRepo) UpdateAutoStatus(ctx context.Context, date string, blockID string, status string) error {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return err
	}
	_, err = r.db.ExecContext(ctx,
		`UPDATE time_block_states SET auto_status=? WHERE entry_id=? AND time_block_id=?`,
		status, entry.ID, blockID)
	return err
}

// RecomputeAutoStatus computes the auto status for one block from the current time
// (same transitions as the periodic updater) and persists it. Returns the status.
func (r *ScheduleRepo) RecomputeAutoStatus(ctx context.Context, date string, blockID string) (string, error) {
	var mode, startTime string
	var endTime sql.NullString
	var duration sql.NullInt64
	err := r.db.QueryRowContext(ctx,
		`SELECT mode, start_time, end_time, duration_min FROM time_blocks WHERE id = ?`, blockID).
		Scan(&mode, &startTime, &endTime, &duration)
	if err != nil {
		return "", fmt.Errorf("recompute auto status: get block: %w", err)
	}

	now := time.Now()
	current := now.Format("15:04")
	status := "pending"
	if startTime <= current {
		if mode == "start_end" && endTime.Valid {
			if endTime.String <= current {
				status = "completed"
			} else {
				status = "in_progress"
			}
		} else if mode == "start_duration" && duration.Valid && duration.Int64 > 0 {
			start, perr := time.Parse("15:04", startTime)
			if perr != nil {
				return "", fmt.Errorf("recompute auto status: parse start: %w", perr)
			}
			end := start.Add(time.Duration(duration.Int64) * time.Minute).Format("15:04")
			if end <= current {
				status = "completed"
			} else {
				status = "in_progress"
			}
		} else {
			status = "in_progress"
		}
	}

	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return "", err
	}
	_, err = r.db.ExecContext(ctx,
		`UPDATE time_block_states SET auto_status=? WHERE entry_id=? AND time_block_id=?`,
		status, entry.ID, blockID)
	if err != nil {
		return "", fmt.Errorf("recompute auto status: update: %w", err)
	}
	return status, nil
}

func (r *ScheduleRepo) UpdateManualStatus(ctx context.Context, date string, blockID string, status string) error {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return err
	}
	_, err = r.db.ExecContext(ctx,
		`UPDATE time_block_states SET manual_status=? WHERE entry_id=? AND time_block_id=?`,
		status, entry.ID, blockID)
	return err
}

func (r *ScheduleRepo) ToggleSubtask(ctx context.Context, date string, blockID string, subtaskID string) error {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return err
	}

	// Find or create block state
	var stateID string
	err = r.db.QueryRowContext(ctx,
		`SELECT id FROM time_block_states WHERE entry_id=? AND time_block_id=?`, entry.ID, blockID).
		Scan(&stateID)
	if err == sql.ErrNoRows {
		stateID = uuid.New().String()
		_, err = r.db.ExecContext(ctx,
			`INSERT INTO time_block_states (id, entry_id, time_block_id) VALUES (?, ?, ?)`,
			stateID, entry.ID, blockID)
	}
	if err != nil {
		return fmt.Errorf("get block state: %w", err)
	}

	// Toggle subtask — UNIQUE(time_block_state_id, subtask_id) ensures ON CONFLICT fires
	_, err = r.db.ExecContext(ctx,
		`INSERT INTO subtask_states (id, time_block_state_id, subtask_id, done)
		 VALUES (?, ?, ?, 1)
		 ON CONFLICT(time_block_state_id, subtask_id) DO UPDATE SET done = NOT done`,
		uuid.New().String(), stateID, subtaskID)
	return err
}

// ---- Internal helpers ----

func (r *ScheduleRepo) findTemplateForDate(ctx context.Context, dateStr string) (*models.DayTemplate, error) {
	t, err := time.Parse("2006-01-02", dateStr)
	if err != nil {
		return nil, nil
	}
	weekday := int(t.Weekday()) // 0=Sun .. 6=Sat

	templates, err := r.ListTemplates(ctx)
	if err != nil {
		return nil, err
	}

	for _, tmpl := range templates {
		for _, d := range tmpl.RepeatDays {
			if d == weekday {
				return &tmpl, nil
			}
		}
	}
	return nil, nil
}

func (r *ScheduleRepo) createBlockStateTx(ctx context.Context, tx *sql.Tx, entryID string, blockID string) error {
	id := uuid.New().String()
	_, err := tx.ExecContext(ctx,
		`INSERT INTO time_block_states (id, entry_id, time_block_id) VALUES (?, ?, ?)`,
		id, entryID, blockID)
	if err != nil {
		return fmt.Errorf("create block state: %w", err)
	}

	// Create subtask states. Query through the tx: a pool read here would deadlock
	// any setup that pins the number of connections (and is inconsistent with the
	// transaction's own view of the data).
	rows, err := tx.QueryContext(ctx,
		`SELECT id FROM subtasks WHERE time_block_id = ? ORDER BY subtask_order`, blockID)
	if err != nil {
		return fmt.Errorf("create block state: list subtasks: %w", err)
	}
	defer rows.Close()
	for rows.Next() {
		var sid string
		if err := rows.Scan(&sid); err != nil {
			return fmt.Errorf("create block state: scan subtask: %w", err)
		}
		stateID := uuid.New().String()
		if _, err := tx.ExecContext(ctx,
			`INSERT INTO subtask_states (id, time_block_state_id, subtask_id) VALUES (?, ?, ?)`,
			stateID, id, sid); err != nil {
			return fmt.Errorf("create subtask state: %w", err)
		}
	}
	if err := rows.Err(); err != nil {
		return fmt.Errorf("create block state: subtasks: %w", err)
	}
	return nil
}

func (r *ScheduleRepo) createBlockState(ctx context.Context, entryID string, blockID string) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()
	if err := r.createBlockStateTx(ctx, tx, entryID, blockID); err != nil {
		return err
	}
	return tx.Commit()
}

func (r *ScheduleRepo) getBlockStates(ctx context.Context, entryID string, templateID *string) ([]models.BlockState, error) {
	// Get blocks - either from template or from special entry's saved block states
	var rows *sql.Rows
	var err error

	if templateID != nil {
		rows, err = r.db.QueryContext(ctx,
			`SELECT b.id, b.name, b.icon, b.start_time, b.end_time, b.duration_min, b.mode, tb.block_order,
			        COALESCE(tbs.auto_status, 'pending'), COALESCE(tbs.manual_status, 'not_completed'), tbs.id
			 FROM template_blocks tb
			 JOIN time_blocks b ON b.id = tb.block_id
			 LEFT JOIN time_block_states tbs ON b.id = tbs.time_block_id AND tbs.entry_id = ?
			 WHERE tb.template_id = ?
			 ORDER BY tb.block_order`, entryID, *templateID)
	} else {
		rows, err = r.db.QueryContext(ctx,
			`SELECT tb.id, tb.name, tb.icon, tb.start_time, tb.end_time, tb.duration_min, tb.mode, tb.block_order,
			        COALESCE(tbs.auto_status, 'pending'), COALESCE(tbs.manual_status, 'not_completed'), tbs.id
			 FROM time_block_states tbs
			 JOIN time_blocks tb ON tbs.time_block_id = tb.id
			 WHERE tbs.entry_id = ?
			 ORDER BY tb.block_order`, entryID)
	}
	if err != nil {
		return nil, fmt.Errorf("get block states: %w", err)
	}
	defer rows.Close()

	var blocks []models.BlockState
	for rows.Next() {
		var bs models.BlockState
		var stateID sql.NullString
		if err := rows.Scan(&bs.TimeBlockID, &bs.Name, &bs.Icon, &bs.StartTime, &bs.EndTime, &bs.DurationMin, &bs.Mode, &bs.BlockOrder, &bs.AutoStatus, &bs.ManualStatus, &stateID); err != nil {
			return nil, fmt.Errorf("scan block state: %w", err)
		}

		if stateID.Valid {
			bs.ID = stateID.String
		}

		var subRows *sql.Rows
		var subErr error
		if stateID.Valid {
			subRows, subErr = r.db.QueryContext(ctx,
				`SELECT COALESCE(ss.id, ''), s.id, s.name, COALESCE(ss.done, 0), s.subtask_order
				 FROM subtasks s
				 LEFT JOIN subtask_states ss ON ss.subtask_id = s.id AND ss.time_block_state_id = ?
				 WHERE s.time_block_id = ?
				 ORDER BY s.subtask_order`, stateID.String, bs.TimeBlockID)
		} else {
			subRows, subErr = r.db.QueryContext(ctx,
				`SELECT '', id, name, 0, subtask_order
				 FROM subtasks
				 WHERE time_block_id = ?
				 ORDER BY subtask_order`, bs.TimeBlockID)
		}
		if subErr == nil {
			for subRows.Next() {
				var st models.SubtaskState
				subRows.Scan(&st.ID, &st.SubtaskID, &st.Name, &st.Done, &st.Order)
				bs.SubtaskStates = append(bs.SubtaskStates, st)
			}
			subRows.Close()
		}

		blocks = append(blocks, bs)
	}
	return blocks, nil
}

func (r *ScheduleRepo) rebuildBlockStates(ctx context.Context, entryID string, templateID string) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback()

	// Delete existing states
	_, err = tx.ExecContext(ctx,
		`DELETE FROM time_block_states WHERE entry_id = ?`, entryID)
	if err != nil {
		return fmt.Errorf("delete states: %w", err)
	}

	blocks, err := r.ListTemplateBlocks(ctx, templateID)
	if err != nil {
		return err
	}
	for _, b := range blocks {
		if err := r.createBlockStateTx(ctx, tx, entryID, b.ID); err != nil {
			return fmt.Errorf("create block state: %w", err)
		}
	}
	return tx.Commit()
}

// ---- For special days: store blocks inline ----
// When a user edits blocks on a special day, we save them as time_block_states
// pointing to the original time_blocks. For special days that have custom blocks
// added, we keep the reference but mark the entry as special.
