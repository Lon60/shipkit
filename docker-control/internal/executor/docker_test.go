package executor

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMockDockerExecutor_ComposeUp(t *testing.T) {
	tests := []struct {
		name        string
		shouldFail  bool
		expectError bool
	}{
		{
			name:        "successful compose up",
			shouldFail:  false,
			expectError: false,
		},
		{
			name:        "failed compose up",
			shouldFail:  true,
			expectError: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := NewMockDockerExecutor()
			mock.ShouldFailUp = tt.shouldFail

			err := mock.ComposeUp(context.Background(), "test-project", "version: '3'\nservices:{}")

			if tt.expectError {
				assert.Error(t, err)
				assert.Contains(t, err.Error(), "mock compose up failed")
			} else {
				assert.NoError(t, err)
			}
		})
	}
}

func TestMockDockerExecutor_ComposeDown(t *testing.T) {
	tests := []struct {
		name        string
		shouldFail  bool
		expectError bool
	}{
		{
			name:        "successful compose down",
			shouldFail:  false,
			expectError: false,
		},
		{
			name:        "failed compose down",
			shouldFail:  true,
			expectError: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := NewMockDockerExecutor()
			mock.ShouldFailDown = tt.shouldFail

			err := mock.ComposeDown(context.Background(), "test-project")

			if tt.expectError {
				assert.Error(t, err)
				assert.Contains(t, err.Error(), "mock compose down failed")
			} else {
				assert.NoError(t, err)
			}
		})
	}
}

func TestMockDockerExecutor_ComposeStatus(t *testing.T) {
	tests := []struct {
		name        string
		shouldFail  bool
		expectError bool
	}{
		{
			name:        "successful compose status",
			shouldFail:  false,
			expectError: false,
		},
		{
			name:        "failed compose status",
			shouldFail:  true,
			expectError: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := NewMockDockerExecutor()
			mock.ShouldFailStatus = tt.shouldFail

			status, err := mock.ComposeStatus(context.Background(), "test-project")

			if tt.expectError {
				assert.Error(t, err)
				assert.Nil(t, status)
				assert.Contains(t, err.Error(), "mock compose status failed")
			} else {
				assert.NoError(t, err)
				assert.NotNil(t, status)
				assert.Len(t, status.Services, 1)
				assert.Equal(t, "test-service", status.Services[0].Name)
				assert.Equal(t, "running", status.Services[0].State)
				assert.Equal(t, "healthy", status.Services[0].Health)
				assert.Equal(t, []string{"8080:8080/tcp"}, status.Services[0].Ports)
			}
		})
	}
}
