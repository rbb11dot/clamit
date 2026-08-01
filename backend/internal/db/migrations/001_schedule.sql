CREATE TABLE IF NOT EXISTS day_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    icon TEXT NOT NULL DEFAULT '',
    repeat_days TEXT NOT NULL DEFAULT '[]',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS time_blocks (
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
);

-- A block is a library item; templates reference it through this junction
-- (many-to-many). Day-owned snapshot blocks have entry_id set instead.
CREATE TABLE IF NOT EXISTS template_blocks (
    template_id TEXT NOT NULL REFERENCES day_templates(id) ON DELETE CASCADE,
    block_id TEXT NOT NULL REFERENCES time_blocks(id) ON DELETE CASCADE,
    block_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (template_id, block_id)
);

CREATE TABLE IF NOT EXISTS subtasks (
    id TEXT PRIMARY KEY,
    time_block_id TEXT NOT NULL REFERENCES time_blocks(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    subtask_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS schedule_entries (
    id TEXT PRIMARY KEY,
    date TEXT NOT NULL UNIQUE,
    template_id TEXT REFERENCES day_templates(id),
    is_special INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS time_block_states (
    id TEXT PRIMARY KEY,
    entry_id TEXT NOT NULL REFERENCES schedule_entries(id) ON DELETE CASCADE,
    time_block_id TEXT NOT NULL,
    auto_status TEXT NOT NULL DEFAULT 'pending'
        CHECK(auto_status IN ('pending','in_progress','completed')),
    manual_status TEXT NOT NULL DEFAULT 'not_completed'
        CHECK(manual_status IN ('not_completed','completed'))
);

CREATE TABLE IF NOT EXISTS subtask_states (
    id TEXT PRIMARY KEY,
    time_block_state_id TEXT NOT NULL REFERENCES time_block_states(id) ON DELETE CASCADE,
    subtask_id TEXT NOT NULL,
    done INTEGER NOT NULL DEFAULT 0,
    UNIQUE(time_block_state_id, subtask_id)
);

CREATE INDEX IF NOT EXISTS idx_schedule_entries_date ON schedule_entries(date);
CREATE INDEX IF NOT EXISTS idx_time_block_states_entry ON time_block_states(entry_id);
CREATE INDEX IF NOT EXISTS idx_subtask_states_block_state ON subtask_states(time_block_state_id);
CREATE INDEX IF NOT EXISTS idx_subtasks_block ON subtasks(time_block_id);
