package main

import (
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/yourusername/clamit/internal/api"
)

func main() {
	log.SetFlags(log.LstdFlags | log.Lshortfile)

	addr := ":8080"
	if v := os.Getenv("CLAMIT_ADDR"); v != "" {
		addr = v
	}

	mux := http.NewServeMux()
	api.RegisterRoutes(mux)

	srv := &http.Server{
		Addr:    addr,
		Handler: mux,
	}

	go func() {
		log.Printf("clamit server starting on %s", addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("shutting down")
}
