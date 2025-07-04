package config

import (
	"flag"
	"os"
	"path/filepath"
)

const (
	DefaultPort           = "50051"
	DefaultDeploymentsDir = "./deployments"
	DefaultLogLevel       = "info"
)

type Config struct {
	Port           string
	DeploymentsDir string
	LogLevel       string
}

func Load() *Config {
	cfg := &Config{
		Port:           DefaultPort,
		DeploymentsDir: DefaultDeploymentsDir,
		LogLevel:       DefaultLogLevel,
	}

	if port := os.Getenv("DOCKER_CONTROL_PORT"); port != "" {
		cfg.Port = port
	}

	if deploymentsDir := os.Getenv("DOCKER_CONTROL_DEPLOYMENTS_DIR"); deploymentsDir != "" {
		cfg.DeploymentsDir = deploymentsDir
	}

	if logLevel := os.Getenv("DOCKER_CONTROL_LOG_LEVEL"); logLevel != "" {
		cfg.LogLevel = logLevel
	}

	flag.StringVar(&cfg.Port, "port", cfg.Port, "gRPC server port")
	flag.StringVar(&cfg.DeploymentsDir, "deployments-dir", cfg.DeploymentsDir, "Base directory for deployments")
	flag.StringVar(&cfg.LogLevel, "log-level", cfg.LogLevel, "Log level (debug, info, warn, error)")
	flag.Parse()

	if absPath, err := filepath.Abs(cfg.DeploymentsDir); err == nil {
		cfg.DeploymentsDir = absPath
	}

	return cfg
}
