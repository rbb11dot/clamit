package api

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/yourusername/clamit/internal/db"
	"github.com/yourusername/clamit/internal/models"
)

type ScheduleHandler struct {
	repo *db.ScheduleRepo
}

func NewScheduleHandler(repo *db.ScheduleRepo) *ScheduleHandler {
	return &ScheduleHandler{repo: repo}
}

func (h *ScheduleHandler) RegisterRoutes(mux *http.ServeMux) {
	// Templates
	mux.HandleFunc("POST /api/templates", h.createTemplate)
	mux.HandleFunc("GET /api/templates", h.listTemplates)
	mux.HandleFunc("GET /api/templates/{id}", h.getTemplate)
	mux.HandleFunc("PUT /api/templates/{id}", h.updateTemplate)
	mux.HandleFunc("DELETE /api/templates/{id}", h.deleteTemplate)

	// Blocks
	mux.HandleFunc("POST /api/templates/{tid}/blocks", h.createBlock)
	mux.HandleFunc("PUT /api/blocks/{bid}", h.updateBlock)
	mux.HandleFunc("DELETE /api/blocks/{bid}", h.deleteBlock)
	mux.HandleFunc("PUT /api/blocks/{bid}/order", h.reorderBlocks)

	// Subtasks
	mux.HandleFunc("POST /api/blocks/{bid}/subtasks", h.createSubtask)
	mux.HandleFunc("PUT /api/subtasks/{sid}", h.updateSubtask)
	mux.HandleFunc("DELETE /api/subtasks/{sid}", h.deleteSubtask)
	mux.HandleFunc("PUT /api/subtasks/{sid}/order", h.reorderSubtasks)

	// Schedule
	mux.HandleFunc("GET /api/schedule/{date}", h.getEntry)
	mux.HandleFunc("PUT /api/schedule/{date}/template", h.setEntryTemplate)
	mux.HandleFunc("POST /api/schedule/{date}/blocks", h.addSpecialBlock)
	mux.HandleFunc("DELETE /api/schedule/{date}/blocks/{bid}", h.removeSpecialBlock)

	// Create entry (POST to avoid GET side-effects)
	mux.HandleFunc("POST /api/schedule/{date}", h.createEntry)

	// Status
	mux.HandleFunc("PUT /api/schedule/{date}/block/{bid}/toggle", h.toggleSubtask)
	mux.HandleFunc("PATCH /api/schedule/{date}/block/{bid}/manual", h.updateManualStatus)
}

// ---- Templates ----

func (h *ScheduleHandler) createTemplate(w http.ResponseWriter, r *http.Request) {
	var req models.CreateTemplateReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	tmpl, err := h.repo.CreateTemplate(r.Context(), req)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusCreated, tmpl)
}

func (h *ScheduleHandler) listTemplates(w http.ResponseWriter, r *http.Request) {
	templates, err := h.repo.ListTemplates(r.Context())
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	if templates == nil {
		templates = []models.DayTemplate{}
	}
	writeJSON(w, http.StatusOK, templates)
}

func (h *ScheduleHandler) getTemplate(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	tmpl, err := h.repo.GetTemplate(r.Context(), id)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	if tmpl == nil {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "not found"})
		return
	}
	// Also load blocks
	blocks, err := h.repo.ListBlocks(r.Context(), id)
	if err == nil {
		type templateWithBlocks struct {
			models.DayTemplate
			Blocks []models.TimeBlock `json:"blocks"`
		}
		writeJSON(w, http.StatusOK, templateWithBlocks{*tmpl, blocks})
		return
	}
	writeJSON(w, http.StatusOK, tmpl)
}

func (h *ScheduleHandler) updateTemplate(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	var req models.UpdateTemplateReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	tmpl, err := h.repo.UpdateTemplate(r.Context(), id, req)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	if tmpl == nil {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "not found"})
		return
	}
	writeJSON(w, http.StatusOK, tmpl)
}

func (h *ScheduleHandler) deleteTemplate(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if err := h.repo.DeleteTemplate(r.Context(), id); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// ---- Blocks ----

func (h *ScheduleHandler) createBlock(w http.ResponseWriter, r *http.Request) {
	tid := r.PathValue("tid")
	var req models.CreateBlockReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	block, err := h.repo.CreateBlock(r.Context(), tid, req)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusCreated, block)
}

func (h *ScheduleHandler) updateBlock(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("bid")
	var req models.UpdateBlockReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	block, err := h.repo.UpdateBlock(r.Context(), id, req)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	if block == nil {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "not found"})
		return
	}
	writeJSON(w, http.StatusOK, block)
}

func (h *ScheduleHandler) deleteBlock(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("bid")
	if err := h.repo.DeleteBlock(r.Context(), id); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *ScheduleHandler) reorderBlocks(w http.ResponseWriter, r *http.Request) {
	// bid is not used directly; body contains the ordered IDs
	var req models.ReorderReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	if err := h.repo.ReorderBlocks(r.Context(), req.IDs); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	w.WriteHeader(http.StatusOK)
}

// ---- Subtasks ----

func (h *ScheduleHandler) createSubtask(w http.ResponseWriter, r *http.Request) {
	bid := r.PathValue("bid")
	var req models.SubtaskReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	subtask, err := h.repo.CreateSubtask(r.Context(), bid, req)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusCreated, subtask)
}

func (h *ScheduleHandler) updateSubtask(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("sid")
	var req models.UpdateSubtaskReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	subtask, err := h.repo.UpdateSubtask(r.Context(), id, req)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, subtask)
}

func (h *ScheduleHandler) deleteSubtask(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("sid")
	if err := h.repo.DeleteSubtask(r.Context(), id); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *ScheduleHandler) reorderSubtasks(w http.ResponseWriter, r *http.Request) {
	var req models.ReorderReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	if err := h.repo.ReorderSubtasks(r.Context(), req.IDs); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	w.WriteHeader(http.StatusOK)
}

// ---- Schedule ----

func (h *ScheduleHandler) getEntry(w http.ResponseWriter, r *http.Request) {
	date := r.PathValue("date")
	if !validateDate(date) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid date"})
		return
	}
	entry, err := h.repo.GetEntry(r.Context(), date)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	if entry == nil {
		// Return an empty entry with today's date so the app doesn't crash
		writeJSON(w, http.StatusOK, map[string]interface{}{
			"date":      date,
			"isSpecial": false,
			"blocks":    []interface{}{},
		})
		return
	}
	writeJSON(w, http.StatusOK, entry)
}

func (h *ScheduleHandler) createEntry(w http.ResponseWriter, r *http.Request) {
	date := r.PathValue("date")
	if !validateDate(date) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid date"})
		return
	}
	entry, err := h.repo.GetOrCreateEntry(r.Context(), date)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusCreated, entry)
}

func (h *ScheduleHandler) setEntryTemplate(w http.ResponseWriter, r *http.Request) {
	date := r.PathValue("date")
	if !validateDate(date) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid date"})
		return
	}
	var req models.SetTemplateReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	entry, err := h.repo.SetEntryTemplate(r.Context(), date, req.TemplateID)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, entry)
}

func (h *ScheduleHandler) addSpecialBlock(w http.ResponseWriter, r *http.Request) {
	date := r.PathValue("date")
	if !validateDate(date) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid date"})
		return
	}
	var req struct {
		BlockID string `json:"blockId"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	if err := h.repo.AddSpecialBlock(r.Context(), date, req.BlockID); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func (h *ScheduleHandler) removeSpecialBlock(w http.ResponseWriter, r *http.Request) {
	date := r.PathValue("date")
	if !validateDate(date) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid date"})
		return
	}
	bid := r.PathValue("bid")
	if err := h.repo.RemoveSpecialBlock(r.Context(), date, bid); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// ---- Status ----

func (h *ScheduleHandler) toggleSubtask(w http.ResponseWriter, r *http.Request) {
	date := r.PathValue("date")
	if !validateDate(date) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid date"})
		return
	}
	bid := r.PathValue("bid")
	var req struct {
		SubtaskID string `json:"subtaskId"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	if err := h.repo.ToggleSubtask(r.Context(), date, bid, req.SubtaskID); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func (h *ScheduleHandler) updateManualStatus(w http.ResponseWriter, r *http.Request) {
	date := r.PathValue("date")
	if !validateDate(date) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid date"})
		return
	}
	bid := r.PathValue("bid")
	var req models.UpdateManualStatusReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid body"})
		return
	}
	if req.Status != "completed" && req.Status != "not_completed" {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid status"})
		return
	}
	if err := h.repo.UpdateManualStatus(r.Context(), date, bid, req.Status); err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

// ---- Helpers ----

func validateDate(date string) bool {
	_, err := time.Parse("2006-01-02", date)
	return err == nil
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

// extractIDs parses a comma-separated list of IDs from a query param
func extractIDs(r *http.Request, key string) []string {
	raw := r.URL.Query().Get(key)
	if raw == "" {
		return nil
	}
	parts := strings.Split(raw, ",")
	var ids []string
	for _, p := range parts {
		trimmed := strings.TrimSpace(p)
		if trimmed != "" {
			ids = append(ids, trimmed)
		}
	}
	return ids
}
