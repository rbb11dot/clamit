package db

import (
	"context"
	"database/sql"
	"fmt"
	"path/filepath"
	"testing"
	"time"

	"github.com/yourusername/clamit/internal/models"
)

// newTestRepo opens an isolated file-backed database per test (same schema
// bootstrap as db.Open) so the connection pool behaves exactly like production.
func newTestRepo(t *testing.T) *ScheduleRepo {
	t.Helper()
	dsn := fmt.Sprintf("file:%s?_pragma=journal_mode(WAL)&_pragma=foreign_keys(1)",
		filepath.Join(t.TempDir(), "test.db"))
	db, err := sql.Open("sqlite", dsn)
	if err != nil {
		t.Fatalf("open test db: %v", err)
	}
	t.Cleanup(func() { db.Close() })
	if err := migrate(db); err != nil {
		t.Fatalf("migrate: %v", err)
	}
	if err := ensureDayOwnedBlocks(db); err != nil {
		t.Fatalf("ensureDayOwnedBlocks: %v", err)
	}
	return NewScheduleRepo(db)
}

// friday is a deterministic weekday for weekday-matching tests.
func friday(t *testing.T) int {
	t.Helper()
	d, err := time.Parse("2006-01-02", "2026-07-31") // known Friday
	if err != nil {
		t.Fatal(err)
	}
	return int(d.Weekday())
}

func TestCreateAndGetTemplate(t *testing.T) {
	r := newTestRepo(t)

	tmpl, err := r.CreateTemplate(context.Background(), models.CreateTemplateReq{
		Name: "Haftaici", Icon: "briefcase", RepeatDays: []int{1, 2, 3, 4, 5},
	})
	if err != nil {
		t.Fatalf("create template: %v", err)
	}
	if tmpl.ID == "" || tmpl.Name != "Haftaici" || tmpl.Icon != "briefcase" {
		t.Fatalf("unexpected template: %+v", tmpl)
	}
	if len(tmpl.RepeatDays) != 5 || tmpl.RepeatDays[0] != 1 {
		t.Fatalf("repeatDays not persisted: %v", tmpl.RepeatDays)
	}

	got, err := r.GetTemplate(context.Background(), tmpl.ID)
	if err != nil {
		t.Fatalf("get template: %v", err)
	}
	if got == nil || got.Name != tmpl.Name {
		t.Fatalf("get mismatch: %+v", got)
	}

	all, err := r.ListTemplates(context.Background())
	if err != nil {
		t.Fatalf("list templates: %v", err)
	}
	if len(all) != 1 {
		t.Fatalf("expected 1 template, got %d", len(all))
	}
}

func TestUpdateTemplate(t *testing.T) {
	r := newTestRepo(t)
	tmpl, err := r.CreateTemplate(context.Background(), models.CreateTemplateReq{
		Name: "A", Icon: "star", RepeatDays: []int{0},
	})
	if err != nil {
		t.Fatalf("create: %v", err)
	}

	updated, err := r.UpdateTemplate(context.Background(), tmpl.ID, models.UpdateTemplateReq{
		Name:       new("B"),
		RepeatDays: &[]int{5, 6},
	})
	if err != nil {
		t.Fatalf("update: %v", err)
	}
	if updated.Name != "B" || updated.Icon != "star" {
		t.Fatalf("unexpected updated: %+v", updated)
	}
	if len(updated.RepeatDays) != 2 || updated.RepeatDays[1] != 6 {
		t.Fatalf("repeatDays not updated: %v", updated.RepeatDays)
	}

	missing, err := r.UpdateTemplate(context.Background(), "nope", models.UpdateTemplateReq{Name: new("X")})
	if err != nil {
		t.Fatalf("update missing: %v", err)
	}
	if missing != nil {
		t.Fatalf("expected nil for missing template, got %+v", missing)
	}
}

func TestCreateBlockWithSubtasks(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{1}})

	block, err := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "Sabah rutini", Icon: "coffee", Mode: "start_end",
		StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "Kahve"}, {Name: "Yatak"}},
	})
	if err != nil {
		t.Fatalf("create block: %v", err)
	}
	if block.ID == "" || len(block.Subtasks) != 2 {
		t.Fatalf("unexpected block: %+v", block)
	}
	if block.Subtasks[0].Name != "Kahve" || block.Subtasks[1].Name != "Yatak" {
		t.Fatalf("subtask order wrong: %+v", block.Subtasks)
	}
	if block.Subtasks[0].SubtaskOrder != 0 || block.Subtasks[1].SubtaskOrder != 1 {
		t.Fatalf("subtask orders wrong: %+v", block.Subtasks)
	}

	// Second block gets the next order.
	block2, err := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "Calisma", Mode: "start_duration", StartTime: "08:00", DurationMin: new(60),
	})
	if err != nil {
		t.Fatalf("create block2: %v", err)
	}
	if block2.BlockOrder != 1 || block.BlockOrder != 0 {
		t.Fatalf("block orders: %d, %d", block.BlockOrder, block2.BlockOrder)
	}
}

func TestSaveBlockFieldsAndModeSwitch(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{1}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Icon: "coffee", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
	})

	// Rename + switch to duration: end time must be cleared, duration kept.
	updated, err := r.SaveBlock(context.Background(), block.ID, models.UpdateBlockReq{
		Name: new("B"), Mode: new("start_duration"), DurationMin: new(45),
	})
	if err != nil {
		t.Fatalf("save block: %v", err)
	}
	if updated.Name != "B" || updated.Mode != "start_duration" || updated.EndTime != nil {
		t.Fatalf("duration switch wrong: %+v", updated)
	}
	if updated.DurationMin == nil || *updated.DurationMin != 45 {
		t.Fatalf("duration not set: %+v", updated.DurationMin)
	}

	// Switch back: duration cleared, end time set.
	updated, err = r.SaveBlock(context.Background(), block.ID, models.UpdateBlockReq{
		Mode: new("start_end"), EndTime: new("08:15"),
	})
	if err != nil {
		t.Fatalf("save block: %v", err)
	}
	if updated.Mode != "start_end" || updated.EndTime == nil || *updated.EndTime != "08:15" {
		t.Fatalf("end switch wrong: %+v", updated)
	}
	if updated.DurationMin != nil {
		t.Fatalf("duration not cleared: %+v", updated.DurationMin)
	}

	if _, err := r.SaveBlock(context.Background(), "missing", models.UpdateBlockReq{Name: new("X")}); err == nil {
		t.Fatal("expected error for missing block")
	}
}

func TestSaveBlockSubtaskSync(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{1}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "S1"}, {Name: "S2"}, {Name: "S3"}},
	})

	// Rename S1, drop S2, add a new subtask at the end; order S3,S1,new.
	updated, err := r.SaveBlock(context.Background(), block.ID, models.UpdateBlockReq{
		Subtasks: []models.SubtaskSync{
			{ID: &block.Subtasks[2].ID, Name: "S3 renkli"},
			{ID: &block.Subtasks[0].ID, Name: "S1 degisti"},
			{Name: "Yeni"},
		},
	})
	if err != nil {
		t.Fatalf("sync subtasks: %v", err)
	}
	if len(updated.Subtasks) != 3 {
		t.Fatalf("expected 3 subtasks, got %+v", updated.Subtasks)
	}
	want := []string{"S3 renkli", "S1 degisti", "Yeni"}
	for i, s := range updated.Subtasks {
		if s.Name != want[i] || s.SubtaskOrder != i {
			t.Fatalf("subtask %d wrong: %+v (want %q)", i, s, want[i])
		}
	}
	// The renamed subtask keeps its id (toggle history survives).
	if updated.Subtasks[1].ID != block.Subtasks[0].ID {
		t.Fatalf("rename replaced id: %s -> %s", block.Subtasks[0].ID, updated.Subtasks[1].ID)
	}
	// S2 was deleted.
	for _, s := range updated.Subtasks {
		if s.ID == block.Subtasks[1].ID {
			t.Fatal("deleted subtask still present")
		}
	}
}

func TestReorderBlocksAndSubtasks(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{1}})
	b1, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30")})
	b2, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{Name: "B", Mode: "start_end", StartTime: "08:00", EndTime: new("08:30")})

	if err := r.ReorderBlocks(context.Background(), []string{b2.ID, b1.ID}); err != nil {
		t.Fatalf("reorder blocks: %v", err)
	}
	blocks, _ := r.ListBlocks(context.Background(), tmpl.ID)
	if blocks[0].ID != b2.ID || blocks[1].ID != b1.ID {
		t.Fatalf("block order wrong: %v, %v", blocks[0].ID, blocks[1].ID)
	}

	s1, _ := r.CreateSubtask(context.Background(), b1.ID, models.SubtaskReq{Name: "x"})
	s2, _ := r.CreateSubtask(context.Background(), b1.ID, models.SubtaskReq{Name: "y"})
	if err := r.ReorderSubtasks(context.Background(), []string{s2.ID, s1.ID}); err != nil {
		t.Fatalf("reorder subtasks: %v", err)
	}
	subs, _ := r.ListSubtasks(context.Background(), b1.ID)
	if subs[0].ID != s2.ID || subs[1].ID != s1.ID {
		t.Fatalf("subtask order wrong")
	}
}

func TestGetOrCreateEntryMatchesWeekday(t *testing.T) {
	r := newTestRepo(t)
	fri := friday(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{
		Name: "Haftaici", Icon: "briefcase", RepeatDays: []int{fri},
	})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "s1"}},
	})

	entry, err := r.GetOrCreateEntry(context.Background(), "2026-07-31") // Friday
	if err != nil {
		t.Fatalf("get or create: %v", err)
	}
	if entry.TemplateID == nil || *entry.TemplateID != tmpl.ID {
		t.Fatalf("template not attached: %+v", entry)
	}
	if entry.IsSpecial {
		t.Fatal("weekday-matched day must not be special")
	}
	if len(entry.Blocks) != 1 || entry.Blocks[0].TimeBlockID != block.ID {
		t.Fatalf("blocks not loaded: %+v", entry.Blocks)
	}
	if entry.Blocks[0].AutoStatus != "pending" || entry.Blocks[0].ManualStatus != "not_completed" {
		t.Fatalf("default statuses wrong: %+v", entry.Blocks[0])
	}
	if len(entry.Blocks[0].SubtaskStates) != 1 || entry.Blocks[0].SubtaskStates[0].Done {
		t.Fatalf("subtask states wrong: %+v", entry.Blocks[0].SubtaskStates)
	}

	// Same entry returned on second call, not duplicated.
	again, _ := r.GetOrCreateEntry(context.Background(), "2026-07-31")
	if again.ID != entry.ID {
		t.Fatal("entry duplicated")
	}
}

func TestGetOrCreateEntryNoTemplateIsSpecial(t *testing.T) {
	r := newTestRepo(t)
	entry, err := r.GetOrCreateEntry(context.Background(), "2026-08-03") // Monday, no templates
	if err != nil {
		t.Fatalf("get or create: %v", err)
	}
	if entry.TemplateID != nil || !entry.IsSpecial {
		t.Fatalf("expected special day, got %+v", entry)
	}
}

func TestCopyOnWriteDetachPreservesState(t *testing.T) {
	r := newTestRepo(t)
	fri := friday(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{fri}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "s1"}},
	})

	date := "2026-07-31"
	if _, err := r.GetOrCreateEntry(context.Background(), date); err != nil {
		t.Fatalf("entry: %v", err)
	}

	// Mark the day: manual completed + one toggled subtask.
	if err := r.UpdateManualStatus(context.Background(), date, block.ID, "completed"); err != nil {
		t.Fatalf("manual status: %v", err)
	}
	if err := r.ToggleSubtask(context.Background(), date, block.ID, block.Subtasks[0].ID); err != nil {
		t.Fatalf("toggle: %v", err)
	}

	// Detach: the day becomes a standalone special day.
	detached, err := r.SetEntryTemplate(context.Background(), date, nil)
	if err != nil {
		t.Fatalf("detach: %v", err)
	}
	if !detached.IsSpecial || detached.TemplateID != nil {
		t.Fatalf("not special after detach: %+v", detached)
	}
	if len(detached.Blocks) != 1 {
		t.Fatalf("blocks lost after detach: %+v", detached.Blocks)
	}
	b := detached.Blocks[0]
	if b.ManualStatus != "completed" {
		t.Fatalf("manual status not preserved: %+v", b)
	}
	if len(b.SubtaskStates) != 1 || !b.SubtaskStates[0].Done {
		t.Fatalf("subtask toggle not preserved: %+v", b.SubtaskStates)
	}
	// The day-owned copy is a different row from the library block.
	if b.TimeBlockID == block.ID {
		t.Fatal("day block should be a copy, not the library block")
	}

	// The template is untouched.
	tmplBlocks, _ := r.ListBlocks(context.Background(), tmpl.ID)
	if len(tmplBlocks) != 1 || tmplBlocks[0].ID != block.ID {
		t.Fatalf("template mutated by detach: %+v", tmplBlocks)
	}
}

func TestDeleteTemplateDetachesEntries(t *testing.T) {
	r := newTestRepo(t)
	fri := friday(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{fri}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
	})

	date := "2026-07-31"
	if _, err := r.GetOrCreateEntry(context.Background(), date); err != nil {
		t.Fatalf("entry: %v", err)
	}

	if err := r.DeleteTemplate(context.Background(), tmpl.ID); err != nil {
		t.Fatalf("delete template: %v", err)
	}

	entry, err := r.GetEntry(context.Background(), date)
	if err != nil {
		t.Fatalf("get entry: %v", err)
	}
	if entry == nil || !entry.IsSpecial {
		t.Fatalf("entry should have become special, got %+v", entry)
	}
	if len(entry.Blocks) != 1 || entry.Blocks[0].TimeBlockID == block.ID {
		t.Fatalf("day blocks should be copies: %+v", entry.Blocks)
	}
	if entry.Blocks[0].Name != "A" {
		t.Fatalf("block content lost: %+v", entry.Blocks[0])
	}

	if got, _ := r.GetTemplate(context.Background(), tmpl.ID); got != nil {
		t.Fatal("template still exists")
	}
}

func TestAddAndRemoveSpecialBlock(t *testing.T) {
	r := newTestRepo(t)
	fri := friday(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{fri}})
	_, _ = r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{Name: "Lib", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30")})
	extra, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{Name: "Extra", Mode: "start_end", StartTime: "09:00", EndTime: new("09:30")})

	date := "2026-07-31"
	entry, _ := r.GetOrCreateEntry(context.Background(), date)
	if len(entry.Blocks) != 2 {
		t.Fatalf("expected 2 template blocks, got %d", len(entry.Blocks))
	}

	// Adding a block detaches the day (copy-on-write) and adds a day-owned copy.
	if err := r.AddSpecialBlock(context.Background(), date, extra.ID); err != nil {
		t.Fatalf("add special block: %v", err)
	}
	entry, _ = r.GetEntry(context.Background(), date)
	if !entry.IsSpecial {
		t.Fatal("day should be special after add")
	}
	if len(entry.Blocks) != 3 {
		t.Fatalf("expected 3 blocks, got %+v", entry.Blocks)
	}
	var extraBlock *models.BlockState
	for i := range entry.Blocks {
		if entry.Blocks[i].Name == "Extra" && entry.Blocks[i].TimeBlockID != extra.ID {
			extraBlock = &entry.Blocks[i]
		}
	}
	if extraBlock == nil {
		t.Fatalf("extra block should have a day-owned copy: %+v", entry.Blocks)
	}

	// Template untouched: still has 2 library blocks.
	libBlocks, _ := r.ListBlocks(context.Background(), tmpl.ID)
	if len(libBlocks) != 2 {
		t.Fatalf("template mutated: %d blocks", len(libBlocks))
	}

	// Remove the added block from the day.
	if err := r.RemoveSpecialBlock(context.Background(), date, extraBlock.TimeBlockID); err != nil {
		t.Fatalf("remove special block: %v", err)
	}
	entry, _ = r.GetEntry(context.Background(), date)
	if len(entry.Blocks) != 2 {
		t.Fatalf("after remove: %+v", entry.Blocks)
	}
	for _, b := range entry.Blocks {
		if b.Name == "Extra" && b.TimeBlockID == extra.ID {
			t.Fatal("removed copy still present")
		}
	}
}

func TestToggleSubtaskAndManualStatus(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{0}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "s1"}},
	})
	date := "2026-08-02" // Sunday
	if _, err := r.GetOrCreateEntry(context.Background(), date); err != nil {
		t.Fatal(err)
	}

	// Toggle on.
	if err := r.ToggleSubtask(context.Background(), date, block.ID, block.Subtasks[0].ID); err != nil {
		t.Fatalf("toggle on: %v", err)
	}
	entry, _ := r.GetEntry(context.Background(), date)
	if !entry.Blocks[0].SubtaskStates[0].Done {
		t.Fatal("subtask should be done")
	}
	// Toggle off (server-side NOT flip).
	if err := r.ToggleSubtask(context.Background(), date, block.ID, block.Subtasks[0].ID); err != nil {
		t.Fatalf("toggle off: %v", err)
	}
	entry, _ = r.GetEntry(context.Background(), date)
	if entry.Blocks[0].SubtaskStates[0].Done {
		t.Fatal("subtask should be undone")
	}

	if err := r.UpdateManualStatus(context.Background(), date, block.ID, "completed"); err != nil {
		t.Fatalf("manual: %v", err)
	}
	entry, _ = r.GetEntry(context.Background(), date)
	if entry.Blocks[0].ManualStatus != "completed" {
		t.Fatalf("manual status not set: %+v", entry.Blocks[0])
	}
}

// windowWithinToday guards wall-clock based auto-status assertions against the
// few seconds where a ±2 minute window crosses midnight.
func windowWithinToday(t *testing.T, start, end time.Time) {
	t.Helper()
	now := time.Now()
	if start.Day() != now.Day() || end.Day() != now.Day() {
		t.Skip("test window crosses midnight; skipping")
	}
}

func TestRecomputeAutoStatusTransitions(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{0}})
	now := time.Now()
	nowStr := now.Format("15:04")

	completed, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "past", Mode: "start_end",
		StartTime: now.Add(-2 * time.Minute).Format("15:04"),
		EndTime:   new(now.Add(-1 * time.Minute).Format("15:04")),
	})
	inProgress, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "now", Mode: "start_duration", StartTime: nowStr, DurationMin: new(60),
	})
	pending, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "future", Mode: "start_end",
		StartTime: now.Add(2 * time.Minute).Format("15:04"),
		EndTime:   new(now.Add(3 * time.Minute).Format("15:04")),
	})
	date := "2026-08-02"
	if _, err := r.GetOrCreateEntry(context.Background(), date); err != nil {
		t.Fatal(err)
	}

	windowWithinToday(t, now.Add(-2*time.Minute), now.Add(3*time.Minute))

	cases := []struct {
		blockID string
		want    string
	}{
		{completed.ID, "completed"},
		{inProgress.ID, "in_progress"},
		{pending.ID, "pending"},
	}
	for _, c := range cases {
		got, err := r.RecomputeAutoStatus(context.Background(), date, c.blockID)
		if err != nil {
			t.Fatalf("recompute %s: %v", c.want, err)
		}
		if got != c.want {
			t.Fatalf("block %s: got %s, want %s", c.blockID, got, c.want)
		}
	}
}

func TestSaveEntryBlockOnSpecialDay(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{0}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "s1"}, {Name: "s2"}},
	})
	date := "2026-08-02"
	entry, _ := r.GetOrCreateEntry(context.Background(), date)
	dayBlockID := entry.Blocks[0].TimeBlockID

	// Toggle one subtask, then edit the day's block: rename, drop s2, add new.
	sub1 := block.Subtasks[0].ID
	if err := r.ToggleSubtask(context.Background(), date, dayBlockID, sub1); err != nil {
		t.Fatal(err)
	}
	updated, err := r.SaveEntryBlock(context.Background(), date, dayBlockID, models.UpdateBlockReq{
		Name:      new("A edited"),
		StartTime: new("06:30"),
		Subtasks: []models.SubtaskSync{
			{ID: &sub1, Name: "s1 degisti"},
			{Name: "yeni"},
		},
	})
	if err != nil {
		t.Fatalf("save entry block: %v", err)
	}
	b := updated.Blocks[0]
	if b.Name != "A edited" || b.StartTime != "06:30" {
		t.Fatalf("fields not updated: %+v", b)
	}
	if len(b.SubtaskStates) != 2 {
		t.Fatalf("expected 2 subtasks, got %+v", b.SubtaskStates)
	}
	if !b.SubtaskStates[0].Done {
		t.Fatalf("toggle lost after rename: %+v", b.SubtaskStates)
	}
	if b.SubtaskStates[0].Name != "s1 degisti" {
		t.Fatalf("rename not applied: %+v", b.SubtaskStates)
	}

	// Library block untouched.
	lib, _ := r.GetBlock(context.Background(), block.ID)
	if lib.Name != "A" || len(lib.Subtasks) != 2 {
		t.Fatalf("library block mutated: %+v", lib)
	}
}

func TestSaveEntryBlockOnTemplateDayDetaches(t *testing.T) {
	r := newTestRepo(t)
	fri := friday(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{fri}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "s1"}},
	})
	date := "2026-07-31"
	entry, _ := r.GetOrCreateEntry(context.Background(), date)
	if entry.TemplateID == nil {
		t.Fatal("precondition: day should be template-linked")
	}

	// Edit via the library block id: the day must detach into a special day,
	// and the edit must land on the day-owned copy, not the template.
	updated, err := r.SaveEntryBlock(context.Background(), date, block.ID, models.UpdateBlockReq{
		Name: new("Day only edit"),
		Subtasks: []models.SubtaskSync{
			{ID: &block.Subtasks[0].ID, Name: "s1 day"},
		},
	})
	if err != nil {
		t.Fatalf("save entry block: %v", err)
	}
	if !updated.IsSpecial || updated.TemplateID != nil {
		t.Fatalf("day should be special after edit: %+v", updated)
	}
	if updated.Blocks[0].Name != "Day only edit" {
		t.Fatalf("edit not applied: %+v", updated.Blocks[0])
	}
	if updated.Blocks[0].SubtaskStates[0].Name != "s1 day" {
		t.Fatalf("subtask edit not applied: %+v", updated.Blocks[0].SubtaskStates)
	}

	lib, _ := r.GetBlock(context.Background(), block.ID)
	if lib.Name != "A" || lib.Subtasks[0].Name != "s1" {
		t.Fatalf("template block mutated: %+v", lib)
	}
}

func TestSetEntryTemplateAttach(t *testing.T) {
	r := newTestRepo(t)
	fri := friday(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{fri}})
	if _, err := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
	}); err != nil {
		t.Fatal(err)
	}

	date := "2026-08-02" // Sunday — no template match, special day
	if _, err := r.GetOrCreateEntry(context.Background(), date); err != nil {
		t.Fatal(err)
	}

	attached, err := r.SetEntryTemplate(context.Background(), date, &tmpl.ID)
	if err != nil {
		t.Fatalf("attach: %v", err)
	}
	if attached.TemplateID == nil || *attached.TemplateID != tmpl.ID || attached.IsSpecial {
		t.Fatalf("attach failed: %+v", attached)
	}
	if len(attached.Blocks) != 1 {
		t.Fatalf("blocks not rebuilt: %+v", attached.Blocks)
	}

	// Re-attach to a different template: states are rebuilt from the new template.
	tmpl2, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T2", Icon: "star", RepeatDays: []int{0}})
	if _, err := r.CreateBlock(context.Background(), tmpl2.ID, models.CreateBlockReq{
		Name: "B", Mode: "start_end", StartTime: "10:00", EndTime: new("10:30"),
	}); err != nil {
		t.Fatal(err)
	}
	replaced, err := r.SetEntryTemplate(context.Background(), date, &tmpl2.ID)
	if err != nil {
		t.Fatalf("replace: %v", err)
	}
	if len(replaced.Blocks) != 1 || replaced.Blocks[0].Name != "B" {
		t.Fatalf("replace failed: %+v", replaced.Blocks)
	}
}

func TestDeleteBlockCascadesSubtasks(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{1}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "s1"}},
	})
	if err := r.DeleteBlock(context.Background(), block.ID); err != nil {
		t.Fatalf("delete block: %v", err)
	}
	subs, _ := r.ListSubtasks(context.Background(), block.ID)
	if len(subs) != 0 {
		t.Fatalf("subtasks not cascaded: %v", subs)
	}
	if got, _ := r.GetBlock(context.Background(), block.ID); got != nil {
		t.Fatal("block still present")
	}
}

func TestUpdateAndDeleteSubtask(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{1}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
	})
	sub, _ := r.CreateSubtask(context.Background(), block.ID, models.SubtaskReq{Name: "s1"})

	updated, err := r.UpdateSubtask(context.Background(), sub.ID, models.UpdateSubtaskReq{Name: new("s1 v2")})
	if err != nil {
		t.Fatalf("update subtask: %v", err)
	}
	if updated.Name != "s1 v2" {
		t.Fatalf("update: %+v", updated)
	}

	if err := r.DeleteSubtask(context.Background(), sub.ID); err != nil {
		t.Fatalf("delete subtask: %v", err)
	}
	if subs, _ := r.ListSubtasks(context.Background(), block.ID); len(subs) != 0 {
		t.Fatalf("subtask still present: %v", subs)
	}
}

func TestUpdateAutoStatus(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{0}})
	block, _ := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
	})
	date := "2026-08-02"
	if _, err := r.GetOrCreateEntry(context.Background(), date); err != nil {
		t.Fatal(err)
	}

	if err := r.UpdateAutoStatus(context.Background(), date, block.ID, "in_progress"); err != nil {
		t.Fatalf("update auto: %v", err)
	}
	entry, _ := r.GetEntry(context.Background(), date)
	if entry.Blocks[0].AutoStatus != "in_progress" {
		t.Fatalf("auto status not set: %+v", entry.Blocks[0])
	}
}

// TestOpenMigratesLegacySchema simulates a database created before day-owned
// blocks existed (time_blocks without entry_id) and verifies db.Open rebuilds
// the table, preserving existing rows.
func TestOpenMigratesLegacySchema(t *testing.T) {
	legacyPath := filepath.Join(t.TempDir(), "legacy.db")
	legacy, err := sql.Open("sqlite", legacyPath)
	if err != nil {
		t.Fatal(err)
	}
	// Old schema: template-owned blocks only, no entry_id column.
	_, err = legacy.Exec(`
		CREATE TABLE day_templates (id TEXT PRIMARY KEY, name TEXT NOT NULL, icon TEXT NOT NULL DEFAULT '', repeat_days TEXT NOT NULL DEFAULT '[]', created_at TEXT NOT NULL DEFAULT (datetime('now')));
		CREATE TABLE time_blocks (id TEXT PRIMARY KEY, template_id TEXT NOT NULL REFERENCES day_templates(id) ON DELETE CASCADE, name TEXT NOT NULL, icon TEXT NOT NULL DEFAULT '', mode TEXT NOT NULL CHECK(mode IN ('start_end','start_duration')), start_time TEXT NOT NULL, end_time TEXT, duration_min INTEGER, block_order INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL DEFAULT (datetime('now')));
		CREATE TABLE schedule_entries (id TEXT PRIMARY KEY, date TEXT NOT NULL UNIQUE, template_id TEXT REFERENCES day_templates(id), is_special INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL DEFAULT (datetime('now')));
		INSERT INTO day_templates (id, name) VALUES ('tmpl1', 'Legacy');
		INSERT INTO time_blocks (id, template_id, name, mode, start_time) VALUES ('blk1', 'tmpl1', 'A', 'start_end', '07:00');
	`)
	if err != nil {
		t.Fatalf("seed legacy db: %v", err)
	}
	legacy.Close()

	database, err := Open(legacyPath)
	if err != nil {
		t.Fatalf("open legacy db: %v", err)
	}
	defer database.Close()

	// entry_id column exists and data survived.
	var hasEntryID bool
	rows, _ := database.Query(`SELECT COUNT(*) FROM pragma_table_info('time_blocks') WHERE name = 'entry_id'`)
	if rows.Next() {
		var n int
		rows.Scan(&n)
		hasEntryID = n == 1
	}
	rows.Close()
	if !hasEntryID {
		t.Fatal("time_blocks not rebuilt with entry_id")
	}
	var name string
	if err := database.QueryRow(`SELECT name FROM time_blocks WHERE id = 'blk1'`).Scan(&name); err != nil {
		t.Fatalf("row lost in migration: %v", err)
	}
	if name != "A" {
		t.Fatalf("row corrupted: %q", name)
	}

	// Day-owned blocks now work: attach a block to an entry.
	repo := NewScheduleRepo(database)
	entry, err := repo.GetOrCreateEntry(context.Background(), "2026-08-02")
	if err != nil {
		t.Fatalf("entry after migration: %v", err)
	}
	if err := repo.AddSpecialBlock(context.Background(), "2026-08-02", "blk1"); err != nil {
		t.Fatalf("add special block after migration: %v", err)
	}
	entry, _ = repo.GetEntry(context.Background(), "2026-08-02")
	if len(entry.Blocks) != 1 {
		t.Fatalf("day-owned block not visible: %+v", entry.Blocks)
	}
}

func TestTemplatesIncludeBlocksAndSubtasks(t *testing.T) {
	r := newTestRepo(t)
	tmpl, _ := r.CreateTemplate(context.Background(), models.CreateTemplateReq{Name: "T", Icon: "star", RepeatDays: []int{1}})
	if _, err := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "A", Mode: "start_end", StartTime: "07:00", EndTime: new("07:30"),
		Subtasks: []models.SubtaskReq{{Name: "s1"}},
	}); err != nil {
		t.Fatal(err)
	}
	if _, err := r.CreateBlock(context.Background(), tmpl.ID, models.CreateBlockReq{
		Name: "B", Mode: "start_duration", StartTime: "08:00", DurationMin: new(30),
	}); err != nil {
		t.Fatal(err)
	}

	// The library UI renders blocks and their subtasks straight from template
	// lists — both must be populated.
	list, err := r.ListTemplates(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	if len(list) != 1 || len(list[0].Blocks) != 2 {
		t.Fatalf("templates missing blocks: %+v", list)
	}
	if list[0].Blocks[0].Name != "A" || len(list[0].Blocks[0].Subtasks) != 1 {
		t.Fatalf("blocks missing subtasks: %+v", list[0].Blocks)
	}

	got, err := r.GetTemplate(context.Background(), tmpl.ID)
	if err != nil {
		t.Fatal(err)
	}
	if len(got.Blocks) != 2 || got.Blocks[1].Name != "B" {
		t.Fatalf("get template missing blocks: %+v", got.Blocks)
	}
}
