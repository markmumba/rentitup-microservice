package xds

import (
	"fmt"
	"time"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	routerv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/router/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	resourcev3 "github.com/envoyproxy/go-control-plane/pkg/resource/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/rentitup/xds-control-plane/internal/eureka"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/durationpb"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

// BuildSnapshot converts Eureka service data into a versioned xDS snapshot.
func BuildSnapshot(services []eureka.Service, version string) (*cachev3.Snapshot, error) {
	var listeners []types.Resource
	var routes    []types.Resource
	var clusters  []types.Resource
	var endpoints []types.Resource

	for _, svc := range services {
		l, err := makeListener(svc.Name)
		if err != nil {
			return nil, fmt.Errorf("listener for %s: %w", svc.Name, err)
		}
		listeners = append(listeners, l)
		routes    = append(routes,    makeRoute(svc.Name))
		clusters  = append(clusters,  makeCluster(svc.Name))
		endpoints = append(endpoints, makeEndpoints(svc.Name, svc.Instances))
	}

	return cachev3.NewSnapshot(version, map[resourcev3.Type][]types.Resource{
		resourcev3.ListenerType: listeners,
		resourcev3.RouteType:    routes,
		resourcev3.ClusterType:  clusters,
		resourcev3.EndpointType: endpoints,
	})
}

// makeListener creates an API listener (gRPC proxyless style) backed by RDS.
func makeListener(name string) (*listenerv3.Listener, error) {
	routerAny, err := anypb.New(&routerv3.Router{})
	if err != nil {
		return nil, err
	}

	manager := &hcmv3.HttpConnectionManager{
		CodecType:  hcmv3.HttpConnectionManager_AUTO,
		StatPrefix: name,
		RouteSpecifier: &hcmv3.HttpConnectionManager_Rds{
			Rds: &hcmv3.Rds{
				ConfigSource: &corev3.ConfigSource{
					ResourceApiVersion: corev3.ApiVersion_V3,
					ConfigSourceSpecifier: &corev3.ConfigSource_Ads{
						Ads: &corev3.AggregatedConfigSource{},
					},
				},
				RouteConfigName: name,
			},
		},
		HttpFilters: []*hcmv3.HttpFilter{
			{
				Name:       wellknown.Router,
				ConfigType: &hcmv3.HttpFilter_TypedConfig{TypedConfig: routerAny},
			},
		},
	}

	managerAny, err := anypb.New(manager)
	if err != nil {
		return nil, err
	}

	return &listenerv3.Listener{
		Name: name,
		ApiListener: &listenerv3.ApiListener{
			ApiListener: managerAny,
		},
	}, nil
}

// makeRoute routes all traffic for a service name to its cluster.
func makeRoute(name string) *routev3.RouteConfiguration {
	return &routev3.RouteConfiguration{
		Name: name,
		VirtualHosts: []*routev3.VirtualHost{
			{
				Name:    name,
				Domains: []string{name, name + ":*"},
				Routes: []*routev3.Route{
					{
						Match: &routev3.RouteMatch{
							PathSpecifier: &routev3.RouteMatch_Prefix{Prefix: "/"},
						},
						Action: &routev3.Route_Route{
							Route: &routev3.RouteAction{
								ClusterSpecifier: &routev3.RouteAction_Cluster{
									Cluster: name,
								},
							},
						},
					},
				},
			},
		},
	}
}

// makeCluster creates an EDS-backed cluster for a service.
func makeCluster(name string) *clusterv3.Cluster {
	return &clusterv3.Cluster{
		Name:                 name,
		ConnectTimeout:       durationpb.New(5 * time.Second),
		ClusterDiscoveryType: &clusterv3.Cluster_Type{Type: clusterv3.Cluster_EDS},
		EdsClusterConfig: &clusterv3.Cluster_EdsClusterConfig{
			EdsConfig: &corev3.ConfigSource{
				ResourceApiVersion: corev3.ApiVersion_V3,
				ConfigSourceSpecifier: &corev3.ConfigSource_Ads{
					Ads: &corev3.AggregatedConfigSource{},
				},
			},
		},
		LbPolicy: clusterv3.Cluster_ROUND_ROBIN,
	}
}

// makeEndpoints creates a ClusterLoadAssignment from Eureka instances.
func makeEndpoints(name string, instances []eureka.ServiceInstance) *endpointv3.ClusterLoadAssignment {
	var lbEndpoints []*endpointv3.LbEndpoint

	for _, inst := range instances {
		lbEndpoints = append(lbEndpoints, &endpointv3.LbEndpoint{
			HostIdentifier: &endpointv3.LbEndpoint_Endpoint{
				Endpoint: &endpointv3.Endpoint{
					Address: &corev3.Address{
						Address: &corev3.Address_SocketAddress{
							SocketAddress: &corev3.SocketAddress{
								Protocol: corev3.SocketAddress_TCP,
								Address:  inst.Host,
								PortSpecifier: &corev3.SocketAddress_PortValue{
									PortValue: uint32(inst.GRPCPort),
								},
							},
						},
					},
				},
			},
			LoadBalancingWeight: wrapperspb.UInt32(1),
		})
	}

	return &endpointv3.ClusterLoadAssignment{
		ClusterName: name,
		Endpoints: []*endpointv3.LocalityLbEndpoints{
			{
				Locality:    &corev3.Locality{Zone: "local"},
				LbEndpoints: lbEndpoints,
			},
		},
	}
}
