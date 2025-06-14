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
	executor executor.DockerExecutor
	logger   *zap.Logger
}

var (
	uuidRegex = regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)

	ErrUUIDRequired       = errors.New("UUID is required")
	ErrUUIDInvalidChars   = errors.New("UUID contains invalid characters")
	ErrUUIDPathSeparators = errors.New("UUID must not contain path separators")
)

func NewDockerControlService(exec executor.DockerExecutor, logger *zap.Logger) *DockerControlService {
	return &DockerControlService{
		executor: exec,
		logger:   logger,
	}
}

func (s *DockerControlService) deploymentPath(uuid string) (string, error) {
	if uuid == "" {
		return "", ErrUUIDRequired
	}
	if strings.Contains(uuid, string(filepath.Separator)) {
		return "", ErrUUIDPathSeparators
	}
	if !uuidRegex.MatchString(uuid) {
		return "", ErrUUIDInvalidChars
	}
	return "", nil
}
