package api

import "net/http"

func RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("GET /health", handleHealth)
}

func handleHealth(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"ok"}`))
}
