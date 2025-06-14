package executor

import "context"

type DockerExecutor interface {
	ComposeUp(ctx context.Context, project string, yaml string) error
	ComposeDown(ctx context.Context, project string) error
	ComposeStatus(ctx context.Context, project string) (*ComposeStatus, error)
}

type ComposeStatus struct {
	Services []ServiceStatus `json:"services"`
}

type ServiceStatus struct {
	Name   string   `json:"name"`
	State  string   `json:"state"`
	Health string   `json:"health"`
	Ports  []string `json:"ports"`
}
