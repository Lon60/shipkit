package executor

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"strings"
)

func buildDockerEnv() []string {
	env := os.Environ()
	if os.Getenv("DOCKER_HOST") == "" {
		env = append(env, "DOCKER_HOST=unix:///var/run/docker.sock")
	}
	return env
}

type DockerComposeExecutor struct{}

func NewDockerComposeExecutor() *DockerComposeExecutor {
	return &DockerComposeExecutor{}
}

func (e *DockerComposeExecutor) ComposeUp(ctx context.Context, project string, yaml string) error {
	cmd := exec.CommandContext(ctx, "docker", "compose", "-p", project, "-f", "-", "up", "-d")
	cmd.Env = buildDockerEnv()
	cmd.Stdin = bytes.NewBufferString(yaml)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("docker compose up failed: %w - output: %s", err, string(output))
	}
	return nil
}

func (e *DockerComposeExecutor) ComposeDown(ctx context.Context, project string) error {
	cmd := exec.CommandContext(ctx, "docker", "compose", "-p", project, "down", "--remove-orphans")
	cmd.Env = buildDockerEnv()
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("docker compose down failed: %w - output: %s", err, string(output))
	}
	return nil
}

func (e *DockerComposeExecutor) ComposeStatus(ctx context.Context, project string) (*ComposeStatus, error) {
	cmd := exec.CommandContext(ctx, "docker", "compose", "-p", project, "ps", "--format", "json")
	cmd.Env = buildDockerEnv()
	output, err := cmd.CombinedOutput()
	if err != nil {
		return nil, fmt.Errorf("docker compose ps failed: %w - output: %s", err, string(output))
	}
	trimmed := strings.TrimSpace(string(output))

	var services []ServiceStatus

	var arr []map[string]any
	if json.Unmarshal([]byte(trimmed), &arr) == nil {
		for _, elem := range arr {
			services = append(services, mapToService(elem))
		}
	} else {
		for _, line := range strings.Split(trimmed, "\n") {
			line = strings.TrimSpace(line)
			if line == "" {
				continue
			}
			var m map[string]any
			if json.Unmarshal([]byte(line), &m) != nil {
				continue
			}
			services = append(services, mapToService(m))
		}
	}

	return &ComposeStatus{Services: services}, nil
}

func mapToService(elem map[string]any) ServiceStatus {
	var name, state, health string
	var ports []string

	if n, ok := elem["Name"].(string); ok {
		name = n
	}
	if s, ok := elem["State"].(string); ok {
		state = s
	}
	if h, ok := elem["Health"].(string); ok {
		health = h
	}

	if pubs, ok := elem["Publishers"].([]any); ok {
		for _, p := range pubs {
			if mp, ok := p.(map[string]any); ok {
				if pubPort, ok := mp["PublishedPort"].(float64); ok {
					if tgtPort, ok := mp["TargetPort"].(float64); ok {
						if proto, ok := mp["Protocol"].(string); ok {
							ports = append(ports, fmt.Sprintf("%.0f:%.0f/%s", pubPort, tgtPort, proto))
						}
					}
				}
			}
		}
	}
	return ServiceStatus{Name: name, State: state, Health: health, Ports: ports}
}
