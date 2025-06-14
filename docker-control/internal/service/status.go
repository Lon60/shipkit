package service

import (
	"context"
	"fmt"
	"os"
	"strings"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

func (s *DockerControlService) GetStatus(ctx context.Context, req *pb.GetStatusRequest) (*pb.AppStatus, error) {
	deploymentDir, err := s.deploymentPath(req.Uuid)
	if err != nil {
		return &pb.AppStatus{
			Uuid:    req.Uuid,
			State:   pb.AppState_ERROR,
			Message: err.Error(),
		}, nil
	}

	s.logger.Info("Getting status", zap.String("uuid", req.Uuid))

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
