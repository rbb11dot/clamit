package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/yourusername/clamit/internal/api"
	"github.com/yourusername/clamit/internal/db"
)

func main() {
	log.SetFlags(log.LstdFlags | log.Lshortfile)

	// Database
	dbPath := os.Getenv("CLAMIT_DB_PATH")
	if dbPath == "" {
		dbPath = "./clamit.db"
	}

	database, err := db.Open(dbPath)
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer database.Close()

	// Repos
	scheduleRepo := db.NewScheduleRepo(database)

	// Routes
	mux := http.NewServeMux()
	api.RegisterRoutes(mux, scheduleRepo)

	// Background: auto-status updater
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	api.StartAutoStatusUpdater(ctx, database)

	// Server
	addr := ":8080"
	if v := os.Getenv("CLAMIT_ADDR"); v != "" {
		addr = v
	}

	srv := &http.Server{
		Addr:    addr,
		Handler: mux,
	}

	go func() {
		log.Printf("clamit server starting on %s (db: %s)", addr, dbPath)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("shutting down")
	cancel()
}
