package service

import (
	"path/filepath"
	"regexp"
	"strings"

	"errors"

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

var uuidRegex = regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)

func NewDockerControlService(exec executor.DockerExecutor, logger *zap.Logger, deploymentsDir string) *DockerControlService {
	return &DockerControlService{
		executor:       exec,
		logger:         logger,
		deploymentsDir: deploymentsDir,
	}
}

func (s *DockerControlService) deploymentPath(uuid string) (string, error) {
	if uuid == "" {
		return "", errors.New("UUID is required")
	}
	if strings.Contains(uuid, string(filepath.Separator)) {
		return "", errors.New("UUID must not contain path separators")
	}
	if !uuidRegex.MatchString(uuid) {
		return "", errors.New("UUID contains invalid characters")
	}
	return filepath.Join(s.deploymentsDir, uuid), nil
}
