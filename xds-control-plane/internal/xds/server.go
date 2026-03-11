package xds

import (
	"context"
	"fmt"
	"log"
	"net"

	cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	serverv3 "github.com/envoyproxy/go-control-plane/pkg/server/v3"
	discoverygrpc "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

// staticHash makes every node share the same snapshot (keyed by "default").
// Convenient for development — one snapshot serves all connecting clients.
type staticHash struct{}

func (staticHash) ID(_ interface{ GetId() string }) string { return "default" }

// NewCache creates an ADS-enabled snapshot cache where all nodes share one snapshot.
func NewCache() cachev3.SnapshotCache {
	return cachev3.NewSnapshotCache(true, cachev3.IDHash{}, nil)
}

// SetSnapshot stores the snapshot under the "default" node key.
func SetSnapshot(ctx context.Context, cache cachev3.SnapshotCache, snap *cachev3.Snapshot) error {
	return cache.SetSnapshot(ctx, "default", snap)
}

// Run starts the ADS gRPC server on the given port and blocks until ctx is cancelled.
func Run(ctx context.Context, cache cachev3.SnapshotCache, port int) error {
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		return fmt.Errorf("listening on :%d: %w", port, err)
	}

	grpcServer := grpc.NewServer()

	xdsServer := serverv3.NewServer(ctx, cache, &Callbacks{})
	discoverygrpc.RegisterAggregatedDiscoveryServiceServer(grpcServer, xdsServer)

	// Enable gRPC reflection so grpcurl can inspect the server
	reflection.Register(grpcServer)

	log.Printf("[xDS] ADS server listening on :%d", port)

	go func() {
		<-ctx.Done()
		log.Println("[xDS] shutting down ADS server")
		grpcServer.GracefulStop()
	}()

	return grpcServer.Serve(lis)
}
