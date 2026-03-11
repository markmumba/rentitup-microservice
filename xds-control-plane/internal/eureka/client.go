package eureka

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// ServiceInstance is a single registered instance of a service.
type ServiceInstance struct {
	Host     string
	GRPCPort int
}

// Service is a named service with one or more live instances.
type Service struct {
	Name      string
	Instances []ServiceInstance
}

// Client polls the Eureka REST API.
type Client struct {
	baseURL    string
	httpClient *http.Client
}

func NewClient(eurekaURL string) *Client {
	return &Client{
		baseURL: strings.TrimSuffix(eurekaURL, "/"),
		httpClient: &http.Client{
			Timeout: 5 * time.Second,
		},
	}
}

// eurekaResponse mirrors the Eureka REST JSON structure.
type eurekaResponse struct {
	Applications struct {
		Application []struct {
			Name     string `json:"name"`
			Instance []struct {
				HostName string `json:"hostName"`
				IPAddr   string `json:"ipAddr"`
				Status   string `json:"status"`
				Metadata map[string]string `json:"metadata"`
			} `json:"instance"`
		} `json:"application"`
	} `json:"applications"`
}

// GetServices fetches all UP services and their gRPC ports from Eureka.
func (c *Client) GetServices() ([]Service, error) {
	req, err := http.NewRequest(http.MethodGet, c.baseURL+"/apps", nil)
	if err != nil {
		return nil, fmt.Errorf("building request: %w", err)
	}
	req.Header.Set("Accept", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("calling Eureka: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("eureka returned status %d", resp.StatusCode)
	}

	var payload eurekaResponse
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return nil, fmt.Errorf("decoding response: %w", err)
	}

	var services []Service
	for _, app := range payload.Applications.Application {
		svc := Service{
			Name: strings.ToLower(app.Name), // normalise to lowercase
		}

		for _, inst := range app.Instance {
			if inst.Status != "UP" {
				continue
			}

			grpcPortStr, ok := inst.Metadata["grpc-port"]
			if !ok {
				// service has no gRPC port — skip
				continue
			}

			grpcPort, err := strconv.Atoi(grpcPortStr)
			if err != nil {
				continue
			}

			host := inst.HostName
			if host == "" {
				host = inst.IPAddr
			}

			svc.Instances = append(svc.Instances, ServiceInstance{
				Host:     host,
				GRPCPort: grpcPort,
			})
		}

		if len(svc.Instances) > 0 {
			services = append(services, svc)
		}
	}

	return services, nil
}
