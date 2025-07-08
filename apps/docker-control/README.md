# Docker Control Microservice

A standalone Go-based microservice that provides gRPC interface for managing Docker Compose operations. This service is designed to be invoked by other services (like the gateway-api) to perform Docker operations based on UUID-identified deployments.

## Prerequisites

- Go 1.21 or higher
- Docker with Docker Compose v2
- Protocol Buffers compiler (`protoc`) - for development only

## Building

### From Source

```bash
cd docker-control
go build -o docker-control-server ./cmd/server
```

### Generate Protobuf Code (Development)

If you modify the `.proto` files, regenerate the Go code:

```bash
# Install protobuf generators (one time)
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

# Generate Go code from proto files
export PATH=$PATH:~/go/bin
protoc --go_out=. --go-grpc_out=. proto/docker_control.proto
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DOCKER_CONTROL_PORT` | `50051` | gRPC server port |
| `DOCKER_CONTROL_DEPLOYMENTS_DIR` | `./deployments` | Base directory for deployments |
| `DOCKER_CONTROL_LOG_LEVEL` | `info` | Log level (debug, info, warn, error) |

### CLI Flags

```bash
./docker-control-server -h
Usage of ./docker-control-server:
  -deployments-dir string
        Base directory for deployments (default "./deployments")
  -log-level string
        Log level (debug, info, warn, error) (default "info")
  -port string
        gRPC server port (default "50051")
```

## Running

### Default Configuration

```bash
./docker-control-server
```

### Custom Configuration

```bash
# Using environment variables
export DOCKER_CONTROL_PORT=8080
export DOCKER_CONTROL_DEPLOYMENTS_DIR=/var/lib/docker-control
export DOCKER_CONTROL_LOG_LEVEL=debug
./docker-control-server

# Using CLI flags
./docker-control-server -port 8080 -deployments-dir /var/lib/docker-control -log-level debug
```

## Testing

### Run All Tests

```bash
go test ./...
```

### Run Tests with Coverage

```bash
go test -cover ./...
```

### Run Specific Test Package

```bash
go test ./internal/service/
go test ./internal/executor/
```

## Docker Integration

The service requires Docker and Docker Compose to be installed and accessible:

```bash
# Verify Docker is available
docker --version
docker compose version

# Test Docker access
docker ps
```