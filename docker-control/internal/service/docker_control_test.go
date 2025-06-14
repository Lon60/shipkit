package service

import (
	"context"
	"os"
	"path/filepath"
	"testing"

	"github.com/shipkit/docker-control/internal/executor"
	pb "github.com/shipkit/docker-control/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap"
)

func TestDockerControlService_StartCompose(t *testing.T) {
	tests := []struct {
		name           string
		req            *pb.StartComposeRequest
		mockSetup      func(*executor.MockDockerExecutor)
		expectedResult *pb.ActionResult
	}{
		{
			name: "successful start",
			req: &pb.StartComposeRequest{
				Uuid:        "test-uuid",
				ComposeYaml: "version: '3'\nservices:\n  test:\n    image: nginx",
			},
			mockSetup: func(mock *executor.MockDockerExecutor) {
				mock.ShouldFailUp = false
			},
			expectedResult: &pb.ActionResult{
				Success: true,
				Message: "Compose started successfully",
			},
		},
		{
			name: "empty uuid",
			req: &pb.StartComposeRequest{
				Uuid:        "",
				ComposeYaml: "version: '3'",
			},
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "UUID is required",
				ErrorCode: "INVALID_UUID",
			},
		},
		{
			name: "empty compose yaml",
			req: &pb.StartComposeRequest{
				Uuid:        "test-uuid",
				ComposeYaml: "",
			},
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "Compose YAML is required",
				ErrorCode: "INVALID_COMPOSE_YAML",
			},
		},
		{
			name: "compose up fails",
			req: &pb.StartComposeRequest{
				Uuid:        "test-uuid",
				ComposeYaml: "version: '3'\nservices:\n  test:\n    image: nginx",
			},
			mockSetup: func(mock *executor.MockDockerExecutor) {
				mock.ShouldFailUp = true
			},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "Failed to start compose: mock compose up failed",
				ErrorCode: "COMPOSE_UP_FAILED",
			},
		},
		{
			name: "invalid uuid with path traversal",
			req: &pb.StartComposeRequest{
				Uuid:        "../etc/passwd",
				ComposeYaml: "version: '3'",
			},
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "UUID must not contain path separators",
				ErrorCode: "INVALID_UUID",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			mockExec := executor.NewMockDockerExecutor()
			tt.mockSetup(mockExec)

			logger := zap.NewNop()
			service := NewDockerControlService(mockExec, logger, tmpDir)

			result, err := service.StartCompose(context.Background(), tt.req)

			require.NoError(t, err)
			assert.Equal(t, tt.expectedResult.Success, result.Success)
			assert.Equal(t, tt.expectedResult.Message, result.Message)
			assert.Equal(t, tt.expectedResult.ErrorCode, result.ErrorCode)

			if tt.expectedResult.Success && tt.req.Uuid != "" {
				composeFile := filepath.Join(tmpDir, tt.req.Uuid, "docker-compose.yml")
				content, err := os.ReadFile(composeFile)
				require.NoError(t, err)
				assert.Equal(t, tt.req.ComposeYaml, string(content))
			}
		})
	}
}

func TestDockerControlService_StopApp(t *testing.T) {
	tests := []struct {
		name           string
		req            *pb.StopAppRequest
		setupDir       bool
		mockSetup      func(*executor.MockDockerExecutor)
		expectedResult *pb.ActionResult
	}{
		{
			name: "successful stop",
			req: &pb.StopAppRequest{
				Uuid: "test-uuid",
			},
			setupDir: true,
			mockSetup: func(mock *executor.MockDockerExecutor) {
				mock.ShouldFailDown = false
			},
			expectedResult: &pb.ActionResult{
				Success: true,
				Message: "App stopped successfully",
			},
		},
		{
			name: "empty uuid",
			req: &pb.StopAppRequest{
				Uuid: "",
			},
			setupDir:  false,
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "UUID is required",
				ErrorCode: "INVALID_UUID",
			},
		},
		{
			name: "deployment not found",
			req: &pb.StopAppRequest{
				Uuid: "nonexistent-uuid",
			},
			setupDir:  false,
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "Deployment not found",
				ErrorCode: "DEPLOYMENT_NOT_FOUND",
			},
		},
		{
			name: "compose down fails",
			req: &pb.StopAppRequest{
				Uuid: "test-uuid",
			},
			setupDir: true,
			mockSetup: func(mock *executor.MockDockerExecutor) {
				mock.ShouldFailDown = true
			},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "Failed to stop app: mock compose down failed",
				ErrorCode: "COMPOSE_DOWN_FAILED",
			},
		},
		{
			name: "invalid uuid with path traversal",
			req: &pb.StopAppRequest{
				Uuid: "../../malicious",
			},
			setupDir:  false,
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "UUID must not contain path separators",
				ErrorCode: "INVALID_UUID",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			mockExec := executor.NewMockDockerExecutor()
			tt.mockSetup(mockExec)

			if tt.setupDir && tt.req.Uuid != "" {
				err := os.MkdirAll(filepath.Join(tmpDir, tt.req.Uuid), 0755)
				require.NoError(t, err)
			}

			logger := zap.NewNop()
			service := NewDockerControlService(mockExec, logger, tmpDir)

			result, err := service.StopApp(context.Background(), tt.req)

			require.NoError(t, err)
			assert.Equal(t, tt.expectedResult.Success, result.Success)
			assert.Equal(t, tt.expectedResult.Message, result.Message)
			assert.Equal(t, tt.expectedResult.ErrorCode, result.ErrorCode)
		})
	}
}

func TestDockerControlService_RestartApp(t *testing.T) {
	tests := []struct {
		name           string
		req            *pb.RestartAppRequest
		setupDir       bool
		mockSetup      func(*executor.MockDockerExecutor)
		expectedResult *pb.ActionResult
	}{
		{
			name: "successful restart",
			req: &pb.RestartAppRequest{
				Uuid: "test-uuid",
			},
			setupDir: true,
			mockSetup: func(mock *executor.MockDockerExecutor) {
				mock.ShouldFailRestart = false
			},
			expectedResult: &pb.ActionResult{
				Success: true,
				Message: "App restarted successfully",
			},
		},
		{
			name: "empty uuid",
			req: &pb.RestartAppRequest{
				Uuid: "",
			},
			setupDir:  false,
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "UUID is required",
				ErrorCode: "INVALID_UUID",
			},
		},
		{
			name: "deployment not found",
			req: &pb.RestartAppRequest{
				Uuid: "nonexistent-uuid",
			},
			setupDir:  false,
			mockSetup: func(mock *executor.MockDockerExecutor) {},
			expectedResult: &pb.ActionResult{
				Success:   false,
				Message:   "Deployment not found",
				ErrorCode: "DEPLOYMENT_NOT_FOUND",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			mockExec := executor.NewMockDockerExecutor()
			tt.mockSetup(mockExec)

			if tt.setupDir && tt.req.Uuid != "" {
				err := os.MkdirAll(filepath.Join(tmpDir, tt.req.Uuid), 0755)
				require.NoError(t, err)
			}

			logger := zap.NewNop()
			service := NewDockerControlService(mockExec, logger, tmpDir)

			result, err := service.RestartApp(context.Background(), tt.req)

			require.NoError(t, err)
			assert.Equal(t, tt.expectedResult.Success, result.Success)
			assert.Equal(t, tt.expectedResult.Message, result.Message)
			assert.Equal(t, tt.expectedResult.ErrorCode, result.ErrorCode)
		})
	}
}

func TestDockerControlService_GetStatus(t *testing.T) {
	tests := []struct {
		name            string
		req             *pb.GetStatusRequest
		setupDir        bool
		mockSetup       func(*executor.MockDockerExecutor)
		expectedState   pb.AppState
		expectedMessage string
	}{
		{
			name: "successful status check",
			req: &pb.GetStatusRequest{
				Uuid: "test-uuid",
			},
			setupDir: true,
			mockSetup: func(mock *executor.MockDockerExecutor) {
				mock.ShouldFailStatus = false
				mock.StatusResponse = &executor.ComposeStatus{
					Services: []executor.ServiceStatus{
						{
							Name:   "test-service",
							State:  "running",
							Health: "healthy",
							Ports:  []string{"8080:8080/tcp"},
						},
					},
				}
			},
			expectedState:   pb.AppState_RUNNING,
			expectedMessage: "Status retrieved successfully",
		},
		{
			name: "empty uuid",
			req: &pb.GetStatusRequest{
				Uuid: "",
			},
			setupDir:        false,
			mockSetup:       func(mock *executor.MockDockerExecutor) {},
			expectedState:   pb.AppState_ERROR,
			expectedMessage: "UUID is required",
		},
		{
			name: "deployment not found",
			req: &pb.GetStatusRequest{
				Uuid: "nonexistent-uuid",
			},
			setupDir:        false,
			mockSetup:       func(mock *executor.MockDockerExecutor) {},
			expectedState:   pb.AppState_UNKNOWN,
			expectedMessage: "Deployment not found",
		},
		{
			name: "no running services",
			req: &pb.GetStatusRequest{
				Uuid: "test-uuid",
			},
			setupDir: true,
			mockSetup: func(mock *executor.MockDockerExecutor) {
				mock.ShouldFailStatus = false
				mock.StatusResponse = &executor.ComposeStatus{
					Services: []executor.ServiceStatus{
						{
							Name:   "test-service",
							State:  "stopped",
							Health: "unhealthy",
							Ports:  []string{},
						},
					},
				}
			},
			expectedState:   pb.AppState_STOPPED,
			expectedMessage: "Status retrieved successfully",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			mockExec := executor.NewMockDockerExecutor()
			tt.mockSetup(mockExec)

			if tt.setupDir && tt.req.Uuid != "" {
				err := os.MkdirAll(filepath.Join(tmpDir, tt.req.Uuid), 0755)
				require.NoError(t, err)
			}

			logger := zap.NewNop()
			service := NewDockerControlService(mockExec, logger, tmpDir)

			result, err := service.GetStatus(context.Background(), tt.req)

			require.NoError(t, err)
			assert.Equal(t, tt.req.Uuid, result.Uuid)
			assert.Equal(t, tt.expectedState, result.State)
			assert.Equal(t, tt.expectedMessage, result.Message)

			if tt.expectedState == pb.AppState_RUNNING {
				assert.NotEmpty(t, result.Containers)
				assert.Equal(t, "test-service", result.Containers[0].Name)
				assert.Equal(t, "running", result.Containers[0].State)
			}
		})
	}
}
