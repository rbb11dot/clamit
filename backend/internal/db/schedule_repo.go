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
	_, err := r.db.ExecContext(ctx, `DELETE FROM day_templates WHERE id = ?`, id)
	return err
}

// ---- Time Blocks ----

func (r *ScheduleRepo) CreateBlock(ctx context.Context, templateID string, req models.CreateBlockReq) (*models.TimeBlock, error) {
	id := uuid.New().String()

	_, err := r.db.ExecContext(ctx,
		`INSERT INTO time_blocks (id, template_id, name, icon, mode, start_time, end_time, duration_min, block_order)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, (SELECT COALESCE(MAX(block_order), -1) + 1 FROM time_blocks WHERE template_id = ?))`,
		id, templateID, req.Name, req.Icon, req.Mode, req.StartTime, req.EndTime, req.DurationMin, templateID)
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
		`SELECT id, template_id, name, icon, mode, start_time, end_time, duration_min, block_order, created_at
		 FROM time_blocks WHERE id = ?`, id).
		Scan(&b.ID, &b.TemplateID, &b.Name, &b.Icon, &b.Mode, &b.StartTime, &b.EndTime, &b.DurationMin, &b.BlockOrder, &b.CreatedAt)
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

func (r *ScheduleRepo) ListBlocks(ctx context.Context, templateID string) ([]models.TimeBlock, error) {
	rows, err := r.db.QueryContext(ctx,
		`SELECT id, template_id, name, icon, mode, start_time, end_time, duration_min, block_order, created_at
		 FROM time_blocks WHERE template_id = ? ORDER BY block_order`, templateID)
	if err != nil {
		return nil, fmt.Errorf("list blocks: %w", err)
	}
	defer rows.Close()

	var blocks []models.TimeBlock
	for rows.Next() {
		var b models.TimeBlock
		if err := rows.Scan(&b.ID, &b.TemplateID, &b.Name, &b.Icon, &b.Mode, &b.StartTime, &b.EndTime, &b.DurationMin, &b.BlockOrder, &b.CreatedAt); err != nil {
			return nil, fmt.Errorf("scan block: %w", err)
		}
		blocks = append(blocks, b)
	}
	return blocks, nil
}

func (r *ScheduleRepo) UpdateBlock(ctx context.Context, id string, req models.UpdateBlockReq) (*models.TimeBlock, error) {
	if req.Name != nil {
		_, err := r.db.ExecContext(ctx, `UPDATE time_blocks SET name=? WHERE id=?`, *req.Name, id)
		if err != nil {
			return nil, fmt.Errorf("update block name: %w", err)
		}
	}
	if req.Icon != nil {
		_, err := r.db.ExecContext(ctx, `UPDATE time_blocks SET icon=? WHERE id=?`, *req.Icon, id)
		if err != nil {
			return nil, fmt.Errorf("update block icon: %w", err)
		}
	}
	if req.StartTime != nil {
		_, err := r.db.ExecContext(ctx, `UPDATE time_blocks SET start_time=? WHERE id=?`, *req.StartTime, id)
		if err != nil {
			return nil, fmt.Errorf("update block start: %w", err)
		}
	}
	if req.EndTime != nil {
		_, err := r.db.ExecContext(ctx, `UPDATE time_blocks SET end_time=? WHERE id=?`, *req.EndTime, id)
		if err != nil {
			return nil, fmt.Errorf("update block end: %w", err)
		}
	}
	if req.DurationMin != nil {
		_, err := r.db.ExecContext(ctx, `UPDATE time_blocks SET duration_min=? WHERE id=?`, *req.DurationMin, id)
		if err != nil {
			return nil, fmt.Errorf("update block duration: %w", err)
		}
	}
	return r.GetBlock(ctx, id)
}

func (r *ScheduleRepo) DeleteBlock(ctx context.Context, id string) error {
	_, err := r.db.ExecContext(ctx, `DELETE FROM time_blocks WHERE id = ?`, id)
	return err
}

func (r *ScheduleRepo) ReorderBlocks(ctx context.Context, ids []string) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback()

	for i, id := range ids {
		_, err := tx.ExecContext(ctx, `UPDATE time_blocks SET block_order=? WHERE id=?`, i, id)
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
		blocks, err := r.ListBlocks(ctx, tmpl.ID)
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

	if templateID == nil {
		// Convert to special day
		_, err = r.db.ExecContext(ctx,
			`UPDATE schedule_entries SET template_id=NULL, is_special=1 WHERE id=?`, entry.ID)
	} else {
		_, err = r.db.ExecContext(ctx,
			`UPDATE schedule_entries SET template_id=?, is_special=0 WHERE id=?`, *templateID, entry.ID)
		// Rebuild block states from template
		if err == nil {
			if rebuildErr := r.rebuildBlockStates(ctx, entry.ID, *templateID); rebuildErr != nil {
				return nil, fmt.Errorf("rebuild block states: %w", rebuildErr)
			}
		}
	}
	if err != nil {
		return nil, fmt.Errorf("set template: %w", err)
	}

	return r.GetEntry(ctx, date)
}

func (r *ScheduleRepo) AddSpecialBlock(ctx context.Context, date string, blockID string) error {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return err
	}

	// Mark as special
	if !entry.IsSpecial {
		_, err = r.db.ExecContext(ctx,
			`UPDATE schedule_entries SET is_special=1 WHERE id=?`, entry.ID)
		if err != nil {
			return fmt.Errorf("mark special: %w", err)
		}
	}

	return r.createBlockState(ctx, entry.ID, blockID)
}

func (r *ScheduleRepo) RemoveSpecialBlock(ctx context.Context, date string, blockID string) error {
	entry, err := r.GetOrCreateEntry(ctx, date)
	if err != nil {
		return err
	}

	_, err = r.db.ExecContext(ctx,
		`DELETE FROM time_block_states WHERE entry_id=? AND time_block_id=?`, entry.ID, blockID)
	return err
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

func (r *ScheduleRepo) createBlockState(ctx context.Context, entryID string, blockID string) error {
	id := uuid.New().String()
	_, err := r.db.ExecContext(ctx,
		`INSERT INTO time_block_states (id, entry_id, time_block_id) VALUES (?, ?, ?)`,
		id, entryID, blockID)
	if err != nil {
		return fmt.Errorf("create block state: %w", err)
	}

	// Create subtask states
	subtasks, err := r.ListSubtasks(ctx, blockID)
	if err != nil {
		return err
	}
	for _, s := range subtasks {
		sid := uuid.New().String()
		_, err := r.db.ExecContext(ctx,
			`INSERT INTO subtask_states (id, time_block_state_id, subtask_id) VALUES (?, ?, ?)`,
			sid, id, s.ID)
		if err != nil {
			return fmt.Errorf("create subtask state: %w", err)
		}
	}
	return nil
}

func (r *ScheduleRepo) getBlockStates(ctx context.Context, entryID string, templateID *string) ([]models.BlockState, error) {
	// Get blocks - either from template or from special entry's saved block states
	var rows *sql.Rows
	var err error

	if templateID != nil {
		rows, err = r.db.QueryContext(ctx,
			`SELECT tb.id, tb.name, tb.icon, tb.start_time, tb.end_time, tb.duration_min, tb.mode, tb.block_order,
			        COALESCE(tbs.auto_status, 'pending'), COALESCE(tbs.manual_status, 'not_completed'), tbs.id
			 FROM time_blocks tb
			 LEFT JOIN time_block_states tbs ON tb.id = tbs.time_block_id AND tbs.entry_id = ?
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
			// Load subtask states
			subRows, err := r.db.QueryContext(ctx,
				`SELECT ss.id, ss.subtask_id, s.name, ss.done, s.subtask_order
				 FROM subtask_states ss
				 JOIN subtasks s ON ss.subtask_id = s.id
				 WHERE ss.time_block_state_id = ?
				 ORDER BY s.subtask_order`, stateID.String)
			if err == nil {
				for subRows.Next() {
					var st models.SubtaskState
					subRows.Scan(&st.ID, &st.SubtaskID, &st.Name, &st.Done, &st.Order)
					bs.SubtaskStates = append(bs.SubtaskStates, st)
				}
				subRows.Close()
			}
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

	blocks, err := r.ListBlocks(ctx, templateID)
	if err != nil {
		return err
	}
	for _, b := range blocks {
		if err := r.createBlockState(ctx, entryID, b.ID); err != nil {
			return fmt.Errorf("create block state: %w", err)
		}
	}
	return tx.Commit()
}

// ---- For special days: store blocks inline ----
// When a user edits blocks on a special day, we save them as time_block_states
// pointing to the original time_blocks. For special days that have custom blocks
// added, we keep the reference but mark the entry as special.
