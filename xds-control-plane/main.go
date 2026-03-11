package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	"github.com/rentitup/xds-control-plane/internal/eureka"
	"github.com/rentitup/xds-control-plane/internal/xds"
)

type config struct {
	eurekaURL  string
	xdsPort    int
	pollPeriod time.Duration
}

func configFromEnv() config {
	pollSecs := getEnvInt("POLL_INTERVAL_SECONDS", 10)
	return config{
		eurekaURL:  getEnv("EUREKA_URL", "http://localhost:8081/eureka"),
		xdsPort:    getEnvInt("XDS_PORT", 18000),
		pollPeriod: time.Duration(pollSecs) * time.Second,
	}
}

func main() {
	cfg := configFromEnv()
	log.Printf("[main] xDS control plane starting — eureka=%s  port=%d  poll=%s",
		cfg.eurekaURL, cfg.xdsPort, cfg.pollPeriod)

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	eurekaClient  := eureka.NewClient(cfg.eurekaURL)
	snapshotCache := xds.NewCache()

	// Initial sync before accepting connections
	syncOnce(ctx, eurekaClient, snapshotCache, 0)

	// Background polling
	go pollLoop(ctx, eurekaClient, snapshotCache, cfg.pollPeriod)

	// ADS server — blocks until ctx cancelled
	if err := xds.Run(ctx, snapshotCache, cfg.xdsPort); err != nil {
		log.Fatalf("[main] ADS server stopped: %v", err)
	}
}

func pollLoop(ctx context.Context, ec *eureka.Client, cache cachev3.SnapshotCache, period time.Duration) {
	ticker := time.NewTicker(period)
	defer ticker.Stop()
	version := 1
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			version++
			syncOnce(ctx, ec, cache, version)
		}
	}
}

func syncOnce(ctx context.Context, ec *eureka.Client, cache cachev3.SnapshotCache, version int) {
	services, err := ec.GetServices()
	if err != nil {
		log.Printf("[sync] Eureka fetch failed: %v", err)
		return
	}

	versionStr := strconv.Itoa(version)
	snap, err := xds.BuildSnapshot(services, versionStr)
	if err != nil {
		log.Printf("[sync] snapshot build failed: %v", err)
		return
	}

	if err := xds.SetSnapshot(ctx, cache, snap); err != nil {
		log.Printf("[sync] snapshot push failed: %v", err)
		return
	}

	names := make([]string, len(services))
	for i, s := range services {
		names[i] = s.Name
	}
	log.Printf("[sync] v%s pushed  services=%v", versionStr, names)
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func getEnvInt(key string, fallback int) int {
	if v := os.Getenv(key); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return fallback
}
