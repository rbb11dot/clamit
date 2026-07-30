package api

import (
	"net/http"

	"github.com/yourusername/clamit/internal/db"
)

func RegisterRoutes(mux *http.ServeMux, scheduleRepo *db.ScheduleRepo) {
	// Health
	mux.HandleFunc("GET /api/health", handleHealth)

	// Schedule
	schedule := NewScheduleHandler(scheduleRepo)
	schedule.RegisterRoutes(mux)
}

func handleHealth(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"ok"}`))
}
