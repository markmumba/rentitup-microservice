package xds

import (
	"context"
	"log"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	discoveryv3 "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
)

// Callbacks logs xDS stream lifecycle events.
type Callbacks struct{}

func (c *Callbacks) OnStreamOpen(_ context.Context, id int64, typ string) error {
	log.Printf("[xDS] stream %d opened  type=%s", id, typ)
	return nil
}

func (c *Callbacks) OnStreamClosed(id int64, node *corev3.Node) {
	log.Printf("[xDS] stream %d closed  node=%s", id, nodeID(node))
}

func (c *Callbacks) OnDeltaStreamOpen(_ context.Context, id int64, typ string) error {
	log.Printf("[xDS] delta stream %d opened  type=%s", id, typ)
	return nil
}

func (c *Callbacks) OnDeltaStreamClosed(id int64, node *corev3.Node) {
	log.Printf("[xDS] delta stream %d closed  node=%s", id, nodeID(node))
}

func (c *Callbacks) OnStreamRequest(id int64, req *discoveryv3.DiscoveryRequest) error {
	log.Printf("[xDS] stream %d request  type=%s  node=%s  names=%v",
		id, req.TypeUrl, nodeID(req.Node), req.ResourceNames)
	return nil
}

func (c *Callbacks) OnStreamResponse(_ context.Context, id int64, req *discoveryv3.DiscoveryRequest, resp *discoveryv3.DiscoveryResponse) {
	log.Printf("[xDS] stream %d response  type=%s  version=%s  resources=%d",
		id, resp.TypeUrl, resp.VersionInfo, len(resp.Resources))
}

func (c *Callbacks) OnStreamDeltaRequest(id int64, req *discoveryv3.DeltaDiscoveryRequest) error {
	log.Printf("[xDS] delta stream %d request  type=%s", id, req.TypeUrl)
	return nil
}

func (c *Callbacks) OnStreamDeltaResponse(id int64, _ *discoveryv3.DeltaDiscoveryRequest, resp *discoveryv3.DeltaDiscoveryResponse) {
	log.Printf("[xDS] delta stream %d response  type=%s  resources=%d", id, resp.TypeUrl, len(resp.Resources))
}

func (c *Callbacks) OnFetchRequest(_ context.Context, req *discoveryv3.DiscoveryRequest) error {
	log.Printf("[xDS] fetch request  type=%s", req.TypeUrl)
	return nil
}

func (c *Callbacks) OnFetchResponse(_ *discoveryv3.DiscoveryRequest, resp *discoveryv3.DiscoveryResponse) {
	log.Printf("[xDS] fetch response  type=%s  version=%s", resp.TypeUrl, resp.VersionInfo)
}

func nodeID(n *corev3.Node) string {
	if n == nil {
		return "<nil>"
	}
	return n.Id
}
