package executor

import (
	"context"
	"encoding/json"
	"fmt"
	"os/exec"
	"strings"
)

type DockerComposeExecutor struct{}

func NewDockerComposeExecutor() *DockerComposeExecutor {
	return &DockerComposeExecutor{}
}

func (e *DockerComposeExecutor) ComposeUp(ctx context.Context, workingDir string) error {
	cmd := exec.CommandContext(ctx, "docker", "compose", "up", "-d")
	cmd.Dir = workingDir

	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("docker compose up failed: %w - output: %s", err, string(output))
	}

	return nil
}

func (e *DockerComposeExecutor) ComposeDown(ctx context.Context, workingDir string) error {
	cmd := exec.CommandContext(ctx, "docker", "compose", "down")
	cmd.Dir = workingDir

	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("docker compose down failed: %w - output: %s", err, string(output))
	}

	return nil
}

func (e *DockerComposeExecutor) ComposeRestart(ctx context.Context, workingDir string) error {
	cmd := exec.CommandContext(ctx, "docker", "compose", "restart")
	cmd.Dir = workingDir

	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("docker compose restart failed: %w - output: %s", err, string(output))
	}

	return nil
}

func (e *DockerComposeExecutor) ComposeStatus(ctx context.Context, workingDir string) (*ComposeStatus, error) {
	cmd := exec.CommandContext(ctx, "docker", "compose", "ps", "--format", "json")
	cmd.Dir = workingDir

	output, err := cmd.CombinedOutput()
	if err != nil {
		return nil, fmt.Errorf("docker compose ps failed: %w - output: %s", err, string(output))
	}

	var services []ServiceStatus
	if len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		for _, line := range lines {
			if line == "" {
				continue
			}

			var service struct {
				Name       string `json:"Name"`
				State      string `json:"State"`
				Health     string `json:"Health"`
				Publishers []struct {
					PublishedPort int    `json:"PublishedPort"`
					TargetPort    int    `json:"TargetPort"`
					Protocol      string `json:"Protocol"`
				} `json:"Publishers"`
			}

			if err := json.Unmarshal([]byte(line), &service); err != nil {
				continue
			}

			var ports []string
			for _, pub := range service.Publishers {
				ports = append(ports, fmt.Sprintf("%d:%d/%s", pub.PublishedPort, pub.TargetPort, pub.Protocol))
			}

			services = append(services, ServiceStatus{
				Name:   service.Name,
				State:  service.State,
				Health: service.Health,
				Ports:  ports,
			})
		}
	}

	return &ComposeStatus{Services: services}, nil
}
