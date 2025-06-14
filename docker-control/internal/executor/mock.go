package executor

import (
	"context"
	"errors"
)

type MockDockerExecutor struct {
	ShouldFailUp     bool
	ShouldFailDown   bool
	ShouldFailStatus bool
	StatusResponse   *ComposeStatus
}

func NewMockDockerExecutor() *MockDockerExecutor {
	return &MockDockerExecutor{
		StatusResponse: &ComposeStatus{
			Services: []ServiceStatus{
				{
					Name:   "test-service",
					State:  "running",
					Health: "healthy",
					Ports:  []string{"8080:8080/tcp"},
				},
			},
		},
	}
}

func (m *MockDockerExecutor) ComposeUp(ctx context.Context, project string, yaml string) error {
	if m.ShouldFailUp {
		return errors.New("mock compose up failed")
	}
	return nil
}

func (m *MockDockerExecutor) ComposeDown(ctx context.Context, project string) error {
	if m.ShouldFailDown {
		return errors.New("mock compose down failed")
	}
	return nil
}

func (m *MockDockerExecutor) ComposeStatus(ctx context.Context, project string) (*ComposeStatus, error) {
	if m.ShouldFailStatus {
		return nil, errors.New("mock compose status failed")
	}
	return m.StatusResponse, nil
}
