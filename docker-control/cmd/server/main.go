package main

import (
	"fmt"
	"net"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"syscall"

	"github.com/shipkit/docker-control/internal/config"
	"github.com/shipkit/docker-control/internal/executor"
	"github.com/shipkit/docker-control/internal/service"
	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	cfg := config.Load()

	socketPath := ensureDockerSocket()
	os.Setenv("DOCKER_HOST", "unix://"+socketPath)

	if _, err := exec.LookPath("docker"); err != nil {
		fmt.Fprintf(os.Stderr, "Docker CLI not found in PATH: %v\n", err)
		os.Exit(1)
	}

	logger, err := setupLogger(cfg.LogLevel)
	if err != nil {
		fmt.Printf("Failed to create logger: %v\n", err)
		os.Exit(1)
	}
	defer logger.Sync()

	logger.Info("Starting Docker Control gRPC server",
		zap.String("port", cfg.Port),
		zap.String("log_level", cfg.LogLevel))

	dockerExec := executor.NewDockerComposeExecutor()
	dockerService := service.NewDockerControlService(dockerExec, logger)

	lis, err := net.Listen("tcp", ":"+cfg.Port)
	if err != nil {
		logger.Fatal("Failed to listen", zap.Error(err))
	}

	grpcServer := grpc.NewServer()
	pb.RegisterDockerControlServiceServer(grpcServer, dockerService)
	reflection.Register(grpcServer)

	go func() {
		logger.Info("gRPC server listening", zap.String("address", lis.Addr().String()))
		if err := grpcServer.Serve(lis); err != nil {
			logger.Fatal("Failed to serve gRPC server", zap.Error(err))
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down gRPC server")
	grpcServer.GracefulStop()
	logger.Info("Server stopped")
}

func setupLogger(level string) (*zap.Logger, error) {
	var zapLevel zapcore.Level
	switch level {
	case "debug":
		zapLevel = zapcore.DebugLevel
	case "info":
		zapLevel = zapcore.InfoLevel
	case "warn":
		zapLevel = zapcore.WarnLevel
	case "error":
		zapLevel = zapcore.ErrorLevel
	default:
		zapLevel = zapcore.InfoLevel
	}

	config := zap.Config{
		Level:       zap.NewAtomicLevelAt(zapLevel),
		Development: false,
		Sampling: &zap.SamplingConfig{
			Initial:    100,
			Thereafter: 100,
		},
		Encoding: "json",
		EncoderConfig: zapcore.EncoderConfig{
			TimeKey:        "timestamp",
			LevelKey:       "level",
			NameKey:        "logger",
			CallerKey:      "caller",
			FunctionKey:    zapcore.OmitKey,
			MessageKey:     "message",
			StacktraceKey:  "stacktrace",
			LineEnding:     zapcore.DefaultLineEnding,
			EncodeLevel:    zapcore.LowercaseLevelEncoder,
			EncodeTime:     zapcore.ISO8601TimeEncoder,
			EncodeDuration: zapcore.SecondsDurationEncoder,
			EncodeCaller:   zapcore.ShortCallerEncoder,
		},
		OutputPaths:      []string{"stdout"},
		ErrorOutputPaths: []string{"stderr"},
	}

	return config.Build()
}

func ensureDockerSocket() string {
	const defaultSocket = "/var/run/docker.sock"

	if isValidSocket(defaultSocket) {
		fmt.Fprintf(os.Stderr, "Using default Docker socket: %s\n", defaultSocket)
		return defaultSocket
	}

	homeDir, err := os.UserHomeDir()
	if err == nil {
		altSocket := filepath.Join(homeDir, ".docker", "desktop", "docker.sock")
		if isValidSocket(altSocket) {
			fmt.Fprintf(os.Stderr, "Using alternative Docker socket: %s\n", altSocket)
			return altSocket
		}
	}

	fmt.Fprintf(os.Stderr, "Docker socket not found in known locations (tried %s and user directory).\n", defaultSocket)
	os.Exit(1)
	return ""
}

func isValidSocket(path string) bool {
	info, err := os.Stat(path)
	if err != nil {
		return false
	}
	return info.Mode()&os.ModeSocket != 0
}
