package api

import (
	"fmt"
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/yourusername/clamit/internal/db"
	"github.com/yourusername/clamit/internal/models"
)

// TestPeriodicAutoStatusUpdater exercises the background updater SQL directly:
// today's entries transition pending → in_progress → completed from wall-clock
// time, and other days are never touched.
func TestPeriodicAutoStatusUpdater(t *testing.T) {
	database, err := db.Open(t.TempDir() + "/test.db")
	if err != nil {
		t.Fatalf("open db: %v", err)
	}
	defer database.Close()
	repo := db.NewScheduleRepo(database)

	now := time.Now()
	today := now.Format("2006-01-02")
	tmpl, err := repo.CreateTemplate(t.Context(), models.CreateTemplateReq{
		Name: "T", Icon: "star", RepeatDays: []int{int(now.Weekday())},
	})
	if err != nil {
		t.Fatal(err)
	}

	// A block that ended a minute ago, started two minutes ago.
	pastStart := now.Add(-2 * time.Minute).Format("15:04")
	pastEnd := now.Add(-1 * time.Minute).Format("15:04")
	past, err := repo.CreateBlock(t.Context(), models.CreateBlockReq{
		Name: "past", Mode: "start_end", StartTime: pastStart, EndTime: &pastEnd,
	})
	if err != nil {
		t.Fatal(err)
	}
	// A block running now (started 2 minutes ago, ends in 60).
	activeStart := now.Add(-2 * time.Minute).Format("15:04")
	active, err := repo.CreateBlock(t.Context(), models.CreateBlockReq{
		Name: "active", Mode: "start_duration", StartTime: activeStart, DurationMin: new(60),
	})
	if err != nil {
		t.Fatal(err)
	}
	if err := repo.AddTemplateBlock(t.Context(), tmpl.ID, past.ID); err != nil {
		t.Fatal(err)
	}
	if err := repo.AddTemplateBlock(t.Context(), tmpl.ID, active.ID); err != nil {
		t.Fatal(err)
	}

	if _, err := repo.GetOrCreateEntry(t.Context(), today); err != nil {
		t.Fatal(err)
	}

	// The updater only transitions 'pending' → later states.
	if err := updateAutoStatuses(database); err != nil {
		t.Fatalf("updateAutoStatuses: %v", err)
	}

	entry, err := repo.GetEntry(t.Context(), today)
	if err != nil {
		t.Fatal(err)
	}
	statuses := map[string]string{}
	for _, b := range entry.Blocks {
		statuses[b.Name] = b.AutoStatus
	}
	if statuses["past"] != "completed" {
		t.Fatalf("past block: %v", statuses)
	}
	if statuses["active"] != "in_progress" {
		t.Fatalf("active block: %v", statuses)
	}

	// A different day is never touched by the updater.
	other := "2030-01-01"
	if _, err := repo.GetOrCreateEntry(t.Context(), other); err != nil {
		t.Fatal(err)
	}
	entry2, _ := repo.GetEntry(t.Context(), other)
	for _, b := range entry2.Blocks {
		if b.AutoStatus != "pending" {
			t.Fatalf("other-day block status mutated: %v", b.AutoStatus)
		}
	}
}

func TestRemainingBlockAndSubtaskHandlers(t *testing.T) {
	srv := newTestServer(t)
	bid := newBlock(t, srv, `{"name":"A","mode":"start_end","startTime":"07:00","endTime":"07:30"}`)
	tid := newTemplate(t, srv, "T", "[1]", bid)

	// Subtask CRUD.
	sub := doJSON(t, "POST", srv.URL+"/api/blocks/"+bid+"/subtasks", `{"name":"s1"}`, http.StatusCreated)
	sid := sub["id"].(string)
	sub2 := doJSON(t, "POST", srv.URL+"/api/blocks/"+bid+"/subtasks", `{"name":"s2"}`, http.StatusCreated)
	sid2 := sub2["id"].(string)

	renamed := doJSON(t, "PUT", srv.URL+"/api/subtasks/"+sid, `{"name":"s1 v2"}`, http.StatusOK)
	if renamed["name"] != "s1 v2" {
		t.Fatalf("rename subtask: %v", renamed)
	}

	// Reorder: s2 first.
	req, _ := http.NewRequest("PUT", srv.URL+"/api/subtasks/"+sid+"/order",
		strings.NewReader(fmt.Sprintf(`{"ids":["%s","%s"]}`, sid2, sid)))
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("reorder subtasks: %d", resp.StatusCode)
	}

	del := doRaw(t, "DELETE", srv.URL+"/api/subtasks/"+sid, "", http.StatusNoContent)
	del.Body.Close()

	// Delete block.
	del = doRaw(t, "DELETE", srv.URL+"/api/blocks/"+bid, "", http.StatusNoContent)
	del.Body.Close()

	// Block gone; junction row cascaded.
	got := doJSON(t, "GET", srv.URL+"/api/templates/"+tid, "", http.StatusOK)
	if len(got["blocks"].([]interface{})) != 0 {
		t.Fatalf("block not deleted: %v", got)
	}
}

func TestReorderTemplateBlocksHandler(t *testing.T) {
	srv := newTestServer(t)
	b1 := newBlock(t, srv, `{"name":"A","mode":"start_end","startTime":"07:00","endTime":"07:30"}`)
	b2 := newBlock(t, srv, `{"name":"B","mode":"start_end","startTime":"08:00","endTime":"08:30"}`)
	tid := newTemplate(t, srv, "T", "[1]", b1, b2)

	req, _ := http.NewRequest("PUT", srv.URL+"/api/templates/"+tid+"/blocks/order",
		strings.NewReader(fmt.Sprintf(`{"ids":["%s","%s"]}`, b2, b1)))
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("reorder blocks: %d", resp.StatusCode)
	}

	got := doJSON(t, "GET", srv.URL+"/api/templates/"+tid, "", http.StatusOK)
	blocks := got["blocks"].([]interface{})
	if blocks[0].(map[string]interface{})["id"] != b2 {
		t.Fatalf("block order not applied: %v", blocks)
	}
}

func TestCreateEntryHandler(t *testing.T) {
	srv := newTestServer(t)
	entry := doJSON(t, "POST", srv.URL+"/api/schedule/2026-08-03", "", http.StatusCreated)
	if entry["date"] != "2026-08-03" {
		t.Fatalf("create entry: %v", entry)
	}
	// Idempotent: same entry id on second call.
	again := doJSON(t, "POST", srv.URL+"/api/schedule/2026-08-03", "", http.StatusCreated)
	if again["id"] != entry["id"] {
		t.Fatalf("entry duplicated: %v vs %v", entry["id"], again["id"])
	}
}

func TestInvalidBodiesRejected(t *testing.T) {
	srv := newTestServer(t)

	cases := []struct {
		method, url, body string
	}{
		{"POST", "/api/templates", `{broken`},
		{"PUT", "/api/templates/x", `{broken`},
		{"POST", "/api/blocks", `{broken`},
		{"PUT", "/api/blocks/x", `{broken`},
		{"PUT", "/api/templates/x/blocks", `{broken`},
		{"PUT", "/api/schedule/2026-08-03/template", `{broken`},
		{"POST", "/api/schedule/2026-08-03/blocks", `{broken`},
		{"PATCH", "/api/schedule/2026-08-03/blocks/x", `{broken`},
		{"PUT", "/api/schedule/2026-08-03/block/x/toggle", `{broken`},
		{"PATCH", "/api/schedule/2026-08-03/block/x/manual", `{broken`},
	}
	for _, c := range cases {
		req, _ := http.NewRequest(c.method, srv.URL+c.url, strings.NewReader(c.body))
		req.Header.Set("Content-Type", "application/json")
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatalf("%s %s: %v", c.method, c.url, err)
		}
		resp.Body.Close()
		if resp.StatusCode != http.StatusBadRequest {
			t.Fatalf("%s %s: status %d, want 400", c.method, c.url, resp.StatusCode)
		}
	}

	// Missing template returns 404 on update.
	req, _ := http.NewRequest("PUT", srv.URL+"/api/templates/missing", strings.NewReader(`{"name":"X"}`))
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != http.StatusNotFound {
		t.Fatalf("update missing template: %d", resp.StatusCode)
	}
}
