package executor

import (
	"context"

	pb "github.com/shipkit/docker-control/proto"
)

type DockerExecutor interface {
	ComposeUp(ctx context.Context, projectName string, composeYAML string) error
	ComposeDown(ctx context.Context, projectName string) error
	ComposeStatus(ctx context.Context, project string) (*ComposeStatus, error)
	GetStatus(ctx context.Context, projectName string) (*pb.AppStatus, error)
	ReloadNginx(ctx context.Context, containerName string) error
	IssueCertificate(ctx context.Context, domain string) error
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
