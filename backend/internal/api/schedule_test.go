package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/yourusername/clamit/internal/db"
)

func newTestServer(t *testing.T) *httptest.Server {
	t.Helper()
	database, err := db.Open(filepath.Join(t.TempDir(), "test.db"))
	if err != nil {
		t.Fatalf("open db: %v", err)
	}
	t.Cleanup(func() { database.Close() })

	repo := db.NewScheduleRepo(database)
	mux := http.NewServeMux()
	RegisterRoutes(mux, repo)
	srv := httptest.NewServer(mux)
	t.Cleanup(srv.Close)
	return srv
}

func doJSON(t *testing.T, method, url, body string, wantStatus int) map[string]interface{} {
	t.Helper()
	resp := doRaw(t, method, url, body, wantStatus)
	defer resp.Body.Close()
	var out map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil {
		t.Fatalf("%s %s: decode: %v", method, url, err)
	}
	return out
}

func doRaw(t *testing.T, method, url, body string, wantStatus int) *http.Response {
	t.Helper()
	req, err := http.NewRequest(method, url, strings.NewReader(body))
	if err != nil {
		t.Fatalf("new request: %v", err)
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatalf("%s %s: %v", method, url, err)
	}
	if resp.StatusCode != wantStatus {
		resp.Body.Close()
		t.Fatalf("%s %s: status %d, want %d", method, url, resp.StatusCode, wantStatus)
	}
	return resp
}

func TestHealth(t *testing.T) {
	srv := newTestServer(t)
	out := doJSON(t, "GET", srv.URL+"/api/health", "", http.StatusOK)
	if out["status"] != "ok" {
		t.Fatalf("health: %v", out)
	}
}

func TestTemplateCRUD(t *testing.T) {
	srv := newTestServer(t)

	created := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"Haftaici","icon":"briefcase","repeatDays":[1,2,3,4,5]}`, http.StatusCreated)
	id := created["id"].(string)
	if created["name"] != "Haftaici" {
		t.Fatalf("create: %v", created)
	}

	req, _ := http.NewRequest("GET", srv.URL+"/api/templates", nil)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var templates []map[string]interface{}
	json.NewDecoder(resp.Body).Decode(&templates)
	resp.Body.Close()
	if len(templates) != 1 || templates[0]["id"] != id {
		t.Fatalf("list: %v", templates)
	}

	got := doJSON(t, "GET", srv.URL+"/api/templates/"+id, "", http.StatusOK)
	if got["name"] != "Haftaici" {
		t.Fatalf("get: %v", got)
	}

	updated := doJSON(t, "PUT", srv.URL+"/api/templates/"+id,
		`{"name":"Haftasonu","repeatDays":[6,0]}`, http.StatusOK)
	if updated["name"] != "Haftasonu" {
		t.Fatalf("update: %v", updated)
	}

	resp = doRaw(t, "DELETE", srv.URL+"/api/templates/"+id, "", http.StatusNoContent)
	resp.Body.Close()

	req, _ = http.NewRequest("GET", srv.URL+"/api/templates/"+id, nil)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != http.StatusNotFound {
		t.Fatalf("get deleted: %d", resp.StatusCode)
	}
}

func TestCreateBlockAndSubtask(t *testing.T) {
	srv := newTestServer(t)
	tmpl := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"T","icon":"star","repeatDays":[1]}`, http.StatusCreated)
	tid := tmpl["id"].(string)

	block := doJSON(t, "POST", srv.URL+"/api/templates/"+tid+"/blocks",
		`{"name":"Sabah","icon":"coffee","mode":"start_end","startTime":"07:00","endTime":"07:30","subtasks":[{"name":"s1"},{"name":"s2"}]}`,
		http.StatusCreated)
	bid := block["id"].(string)
	if block["name"] != "Sabah" {
		t.Fatalf("block: %v", block)
	}

	sub := doJSON(t, "POST", srv.URL+"/api/blocks/"+bid+"/subtasks",
		`{"name":"s3"}`, http.StatusCreated)
	if sub["name"] != "s3" {
		t.Fatalf("subtask: %v", sub)
	}

	// Update the block: rename + mode switch.
	updated := doJSON(t, "PUT", srv.URL+"/api/blocks/"+bid,
		`{"name":"Sabah v2","mode":"start_duration","durationMin":45}`, http.StatusOK)
	if updated["name"] != "Sabah v2" || updated["mode"] != "start_duration" {
		t.Fatalf("update block: %v", updated)
	}
}

func TestGetEntryAutoCreatesAndWeekdayMatches(t *testing.T) {
	srv := newTestServer(t)
	// 2026-07-31 is a Friday.
	tmpl := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"Haftaici","icon":"briefcase","repeatDays":[5]}`, http.StatusCreated)
	tid := tmpl["id"].(string)
	doJSON(t, "POST", srv.URL+"/api/templates/"+tid+"/blocks",
		`{"name":"A","mode":"start_end","startTime":"07:00","endTime":"07:30"}`, http.StatusCreated)

	entry := doJSON(t, "GET", srv.URL+"/api/schedule/2026-07-31", "", http.StatusOK)
	if entry["templateId"] != tid || entry["isSpecial"] != false {
		t.Fatalf("entry: %v", entry)
	}
	blocks := entry["blocks"].([]interface{})
	if len(blocks) != 1 {
		t.Fatalf("entry blocks: %v", blocks)
	}

	// A day with no matching template is a special day.
	empty := doJSON(t, "GET", srv.URL+"/api/schedule/2026-08-02", "", http.StatusOK)
	if empty["isSpecial"] != true {
		t.Fatalf("empty day: %v", empty)
	}
}

func TestSetEntryTemplateAndSpecialDay(t *testing.T) {
	srv := newTestServer(t)
	tmpl := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"T","icon":"star","repeatDays":[1]}`, http.StatusCreated)
	tid := tmpl["id"].(string)

	// Attach to an otherwise empty day.
	attached := doJSON(t, "PUT", srv.URL+"/api/schedule/2026-08-03/template",
		fmt.Sprintf(`{"templateId":"%s"}`, tid), http.StatusOK)
	if attached["templateId"] != tid {
		t.Fatalf("attach: %v", attached)
	}

	// Detach → special day.
	detached := doJSON(t, "PUT", srv.URL+"/api/schedule/2026-08-03/template",
		`{"templateId":null}`, http.StatusOK)
	if detached["isSpecial"] != true || detached["templateId"] != nil {
		t.Fatalf("detach: %v", detached)
	}
}

func TestSpecialBlockAddRemove(t *testing.T) {
	srv := newTestServer(t)
	tmpl := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"T","icon":"star","repeatDays":[5]}`, http.StatusCreated)
	tid := tmpl["id"].(string)
	block := doJSON(t, "POST", srv.URL+"/api/templates/"+tid+"/blocks",
		`{"name":"Extra","mode":"start_end","startTime":"09:00","endTime":"09:30"}`, http.StatusCreated)
	bid := block["id"].(string)

	doJSON(t, "POST", srv.URL+"/api/schedule/2026-07-31/blocks",
		fmt.Sprintf(`{"blockId":"%s"}`, bid), http.StatusOK)

	entry := doJSON(t, "GET", srv.URL+"/api/schedule/2026-07-31", "", http.StatusOK)
	if entry["isSpecial"] != true {
		t.Fatalf("day should be special: %v", entry)
	}

	// Grab the day-owned copy id and remove it.
	blocks := entry["blocks"].([]interface{})
	dayBlock := blocks[len(blocks)-1].(map[string]interface{})
	resp := doRaw(t, "DELETE", srv.URL+"/api/schedule/2026-07-31/blocks/"+dayBlock["timeBlockId"].(string), "", http.StatusNoContent)
	resp.Body.Close()

	entry = doJSON(t, "GET", srv.URL+"/api/schedule/2026-07-31", "", http.StatusOK)
	if len(entry["blocks"].([]interface{})) != 1 {
		t.Fatalf("remove failed: %v", entry)
	}
}

func TestToggleSubtaskAndManualStatus(t *testing.T) {
	srv := newTestServer(t)
	tmpl := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"T","icon":"star","repeatDays":[5]}`, http.StatusCreated)
	tid := tmpl["id"].(string)
	block := doJSON(t, "POST", srv.URL+"/api/templates/"+tid+"/blocks",
		`{"name":"A","mode":"start_end","startTime":"07:00","endTime":"07:30","subtasks":[{"name":"s1"}]}`,
		http.StatusCreated)
	bid := block["id"].(string)
	subtaskID := block["subtasks"].([]interface{})[0].(map[string]interface{})["id"].(string)

	date := "2026-07-31"
	doJSON(t, "PUT", srv.URL+"/api/schedule/"+date+"/block/"+bid+"/toggle",
		fmt.Sprintf(`{"subtaskId":"%s"}`, subtaskID), http.StatusOK)

	entry := doJSON(t, "GET", srv.URL+"/api/schedule/"+date, "", http.StatusOK)
	b := entry["blocks"].([]interface{})[0].(map[string]interface{})
	ss := b["subtaskStates"].([]interface{})[0].(map[string]interface{})
	if ss["done"] != true {
		t.Fatalf("toggle not applied: %v", ss)
	}

	doJSON(t, "PATCH", srv.URL+"/api/schedule/"+date+"/block/"+bid+"/manual",
		`{"status":"completed"}`, http.StatusOK)
	entry = doJSON(t, "GET", srv.URL+"/api/schedule/"+date, "", http.StatusOK)
	b = entry["blocks"].([]interface{})[0].(map[string]interface{})
	if b["manualStatus"] != "completed" {
		t.Fatalf("manual not applied: %v", b)
	}

	// Invalid status rejected.
	req, _ := http.NewRequest("PATCH", srv.URL+"/api/schedule/"+date+"/block/"+bid+"/manual",
		strings.NewReader(`{"status":"bogus"}`))
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != http.StatusBadRequest {
		t.Fatalf("invalid status: %d", resp.StatusCode)
	}
}

func TestUpdateEntryBlockDetaches(t *testing.T) {
	srv := newTestServer(t)
	tmpl := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"T","icon":"star","repeatDays":[5]}`, http.StatusCreated)
	tid := tmpl["id"].(string)
	block := doJSON(t, "POST", srv.URL+"/api/templates/"+tid+"/blocks",
		`{"name":"A","mode":"start_end","startTime":"07:00","endTime":"07:30","subtasks":[{"name":"s1"}]}`,
		http.StatusCreated)
	bid := block["id"].(string)
	date := "2026-07-31"
	doJSON(t, "GET", srv.URL+"/api/schedule/"+date, "", http.StatusOK)

	// Edit the day's block via the library block id: the day must detach.
	updated := doJSON(t, "PATCH", srv.URL+"/api/schedule/"+date+"/blocks/"+bid,
		`{"name":"Day edit","subtasks":[{"id":"`+block["subtasks"].([]interface{})[0].(map[string]interface{})["id"].(string)+`","name":"s1 day"}]}`,
		http.StatusOK)
	if updated["isSpecial"] != true {
		t.Fatalf("day should be special: %v", updated)
	}
	b := updated["blocks"].([]interface{})[0].(map[string]interface{})
	if b["name"] != "Day edit" {
		t.Fatalf("edit not applied: %v", b)
	}
	ss := b["subtaskStates"].([]interface{})[0].(map[string]interface{})
	if ss["name"] != "s1 day" {
		t.Fatalf("subtask edit not applied: %v", ss)
	}

	// Library block untouched.
	lib := doJSON(t, "GET", srv.URL+"/api/templates/"+tid, "", http.StatusOK)
	blocks := lib["blocks"].([]interface{})
	if len(blocks) != 1 || blocks[0].(map[string]interface{})["name"] != "A" {
		t.Fatalf("template mutated: %v", blocks)
	}
}

func TestAutoStatusRecompute(t *testing.T) {
	srv := newTestServer(t)
	tmpl := doJSON(t, "POST", srv.URL+"/api/templates",
		`{"name":"T","icon":"star","repeatDays":[5]}`, http.StatusCreated)
	tid := tmpl["id"].(string)
	// A block that started 2 minutes ago and ended 1 minute ago → completed.
	now := time.Now()
	start := now.Add(-2 * time.Minute).Format("15:04")
	end := now.Add(-1 * time.Minute).Format("15:04")
	block := doJSON(t, "POST", srv.URL+"/api/templates/"+tid+"/blocks",
		fmt.Sprintf(`{"name":"past","mode":"start_end","startTime":"%s","endTime":"%s"}`, start, end),
		http.StatusCreated)
	bid := block["id"].(string)
	date := "2026-07-31"
	doJSON(t, "GET", srv.URL+"/api/schedule/"+date, "", http.StatusOK)

	out := doJSON(t, "PATCH", srv.URL+"/api/schedule/"+date+"/block/"+bid+"/auto", "", http.StatusOK)
	if out["status"] != "completed" {
		t.Fatalf("auto status: %v", out)
	}
}

func TestInvalidDateRejected(t *testing.T) {
	srv := newTestServer(t)
	for _, path := range []string{
		"/api/schedule/not-a-date",
		"/api/schedule/2026-13-01",
	} {
		req, _ := http.NewRequest("GET", srv.URL+path, nil)
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		resp.Body.Close()
		if resp.StatusCode != http.StatusBadRequest {
			t.Fatalf("%s: status %d, want 400", path, resp.StatusCode)
		}
	}
}
