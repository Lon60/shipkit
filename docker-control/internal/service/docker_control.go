package service

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/shipkit/docker-control/internal/executor"
	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

type DockerControlService struct {
	pb.UnimplementedDockerControlServiceServer
	executor       executor.DockerExecutor
	logger         *zap.Logger
	deploymentsDir string
}

func NewDockerControlService(exec executor.DockerExecutor, logger *zap.Logger, deploymentsDir string) *DockerControlService {
	return &DockerControlService{
		executor:       exec,
		logger:         logger,
		deploymentsDir: deploymentsDir,
	}
}

func (s *DockerControlService) StartCompose(ctx context.Context, req *pb.StartComposeRequest) (*pb.ActionResult, error) {
	if req.Uuid == "" {
		return &pb.ActionResult{
			Success:   false,
			Message:   "UUID is required",
			ErrorCode: "INVALID_UUID",
		}, nil
	}

	if req.ComposeYaml == "" {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Compose YAML is required",
			ErrorCode: "INVALID_COMPOSE_YAML",
		}, nil
	}

	s.logger.Info("Starting compose", zap.String("uuid", req.Uuid))

	deploymentDir := filepath.Join(s.deploymentsDir, req.Uuid)
	if err := os.MkdirAll(deploymentDir, 0755); err != nil {
		s.logger.Error("Failed to create deployment directory",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   "Failed to create deployment directory",
			ErrorCode: "DIRECTORY_CREATE_FAILED",
		}, nil
	}

	composeFile := filepath.Join(deploymentDir, "docker-compose.yml")
	if err := os.WriteFile(composeFile, []byte(req.ComposeYaml), 0644); err != nil {
		s.logger.Error("Failed to write compose file",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   "Failed to write compose file",
			ErrorCode: "FILE_WRITE_FAILED",
		}, nil
	}

	if err := s.executor.ComposeUp(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to start compose",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to start compose: %v", err),
			ErrorCode: "COMPOSE_UP_FAILED",
		}, nil
	}

	s.logger.Info("Successfully started compose", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Success: true,
		Message: "Compose started successfully",
	}, nil
}

func (s *DockerControlService) StopApp(ctx context.Context, req *pb.StopAppRequest) (*pb.ActionResult, error) {
	if req.Uuid == "" {
		return &pb.ActionResult{
			Success:   false,
			Message:   "UUID is required",
			ErrorCode: "INVALID_UUID",
		}, nil
	}

	s.logger.Info("Stopping app", zap.String("uuid", req.Uuid))

	deploymentDir := filepath.Join(s.deploymentsDir, req.Uuid)
	if _, err := os.Stat(deploymentDir); os.IsNotExist(err) {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Deployment not found",
			ErrorCode: "DEPLOYMENT_NOT_FOUND",
		}, nil
	}

	if err := s.executor.ComposeDown(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to stop app",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to stop app: %v", err),
			ErrorCode: "COMPOSE_DOWN_FAILED",
		}, nil
	}

	s.logger.Info("Successfully stopped app", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Success: true,
		Message: "App stopped successfully",
	}, nil
}

func (s *DockerControlService) RestartApp(ctx context.Context, req *pb.RestartAppRequest) (*pb.ActionResult, error) {
	if req.Uuid == "" {
		return &pb.ActionResult{
			Success:   false,
			Message:   "UUID is required",
			ErrorCode: "INVALID_UUID",
		}, nil
	}

	s.logger.Info("Restarting app", zap.String("uuid", req.Uuid))

	deploymentDir := filepath.Join(s.deploymentsDir, req.Uuid)
	if _, err := os.Stat(deploymentDir); os.IsNotExist(err) {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Deployment not found",
			ErrorCode: "DEPLOYMENT_NOT_FOUND",
		}, nil
	}

	if err := s.executor.ComposeRestart(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to restart app",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to restart app: %v", err),
			ErrorCode: "COMPOSE_RESTART_FAILED",
		}, nil
	}

	s.logger.Info("Successfully restarted app", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Success: true,
		Message: "App restarted successfully",
	}, nil
}

func (s *DockerControlService) UpdateCompose(ctx context.Context, req *pb.UpdateComposeRequest) (*pb.ActionResult, error) {
	if req.Uuid == "" {
		return &pb.ActionResult{
			Success:   false,
			Message:   "UUID is required",
			ErrorCode: "INVALID_UUID",
		}, nil
	}

	if req.ComposeYaml == "" {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Compose YAML is required",
			ErrorCode: "INVALID_COMPOSE_YAML",
		}, nil
	}

	s.logger.Info("Updating compose", zap.String("uuid", req.Uuid))

	deploymentDir := filepath.Join(s.deploymentsDir, req.Uuid)
	if _, err := os.Stat(deploymentDir); os.IsNotExist(err) {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Deployment not found",
			ErrorCode: "DEPLOYMENT_NOT_FOUND",
		}, nil
	}

	composeFile := filepath.Join(deploymentDir, "docker-compose.yml")
	if err := os.WriteFile(composeFile, []byte(req.ComposeYaml), 0644); err != nil {
		s.logger.Error("Failed to update compose file",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   "Failed to update compose file",
			ErrorCode: "FILE_WRITE_FAILED",
		}, nil
	}

	if err := s.executor.ComposeDown(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to stop app during update",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to stop app during update: %v", err),
			ErrorCode: "COMPOSE_DOWN_FAILED",
		}, nil
	}

	if err := s.executor.ComposeUp(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to start app after update",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to start app after update: %v", err),
			ErrorCode: "COMPOSE_UP_FAILED",
		}, nil
	}

	s.logger.Info("Successfully updated compose", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Success: true,
		Message: "Compose updated successfully",
	}, nil
}

func (s *DockerControlService) GetStatus(ctx context.Context, req *pb.GetStatusRequest) (*pb.AppStatus, error) {
	if req.Uuid == "" {
		return &pb.AppStatus{
			Uuid:    req.Uuid,
			State:   pb.AppState_ERROR,
			Message: "UUID is required",
		}, nil
	}

	s.logger.Info("Getting status", zap.String("uuid", req.Uuid))

	deploymentDir := filepath.Join(s.deploymentsDir, req.Uuid)
	if _, err := os.Stat(deploymentDir); os.IsNotExist(err) {
		return &pb.AppStatus{
			Uuid:    req.Uuid,
			State:   pb.AppState_UNKNOWN,
			Message: "Deployment not found",
		}, nil
	}

	status, err := s.executor.ComposeStatus(ctx, deploymentDir)
	if err != nil {
		s.logger.Error("Failed to get status",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.AppStatus{
			Uuid:    req.Uuid,
			State:   pb.AppState_ERROR,
			Message: fmt.Sprintf("Failed to get status: %v", err),
		}, nil
	}

	var containers []*pb.ContainerStatus
	appState := pb.AppState_STOPPED
	hasRunning := false

	for _, service := range status.Services {
		containers = append(containers, &pb.ContainerStatus{
			Name:   service.Name,
			State:  service.State,
			Health: service.Health,
			Ports:  service.Ports,
		})

		if strings.Contains(strings.ToLower(service.State), "running") {
			hasRunning = true
		}
	}

	if hasRunning {
		appState = pb.AppState_RUNNING
	}

	return &pb.AppStatus{
		Uuid:       req.Uuid,
		State:      appState,
		Containers: containers,
		Message:    "Status retrieved successfully",
	}, nil
}
