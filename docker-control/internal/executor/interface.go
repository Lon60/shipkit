package executor

import "context"

type DockerExecutor interface {
	ComposeUp(ctx context.Context, workingDir string) error
	ComposeDown(ctx context.Context, workingDir string) error
	ComposeRestart(ctx context.Context, workingDir string) error
	ComposeStatus(ctx context.Context, workingDir string) (*ComposeStatus, error)
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
