package service

import (
	"context"
	"fmt"
	"strings"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

func (s *DockerControlService) StopApp(ctx context.Context, req *pb.StopAppRequest) (*pb.ActionResult, error) {
	_, err := s.deploymentPath(req.Uuid)
	if err != nil {
		return &pb.ActionResult{
			Status:  1,
			Message: err.Error(),
			Details: "UUID validation failed",
		}, nil
	}

	s.logger.Info("Stopping app", zap.String("uuid", req.Uuid))

	status, err := s.executor.ComposeStatus(ctx, req.Uuid)
	if err != nil {
		s.logger.Error("Failed to check app status before stopping",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Status:  1,
			Message: "Failed to check app status",
			Details: err.Error(),
		}, nil
	}

	s.logger.Debug("Status check result",
		zap.String("uuid", req.Uuid),
		zap.Int("service_count", len(status.Services)))

	if len(status.Services) == 0 {
		s.logger.Info("App has no services", zap.String("uuid", req.Uuid))
		return &pb.ActionResult{
			Status:  1,
			Message: "App not found",
			Details: fmt.Sprintf("Project %s has no services or does not exist", req.Uuid),
		}, nil
	}

	hasRunning := false
	for _, service := range status.Services {
		s.logger.Debug("Service status",
			zap.String("uuid", req.Uuid),
			zap.String("service", service.Name),
			zap.String("state", service.State))
		if strings.Contains(strings.ToLower(service.State), "running") {
			hasRunning = true
			break
		}
	}

	if !hasRunning {
		s.logger.Info("App is not running", zap.String("uuid", req.Uuid))
		return &pb.ActionResult{
			Status:  1,
			Message: "App is not running",
			Details: fmt.Sprintf("Project %s has no running containers", req.Uuid),
		}, nil
	}

	if err := s.executor.ComposeDown(ctx, req.Uuid); err != nil {
		s.logger.Error("Failed to stop app",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Status:  1,
			Message: "Failed to stop app",
			Details: err.Error(),
		}, nil
	}

	s.logger.Info("Successfully stopped app", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Status:  0,
		Message: "App stopped successfully",
		Details: fmt.Sprintf("Project %s has been stopped", req.Uuid),
	}, nil
}
