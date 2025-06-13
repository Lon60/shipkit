.PHONY: build test clean proto help run run-debug

help:
	@echo "Available targets:"
	@echo "  build      - Build the docker-control-server binary"
	@echo "  test       - Run all tests"
	@echo "  test-cover - Run tests with coverage"
	@echo "  proto      - Generate protobuf Go code"
	@echo "  clean      - Remove built binaries"
	@echo "  tidy       - Download and organize dependencies"
	@echo "  run        - Build and run the server"
	@echo "  help       - Show this help message"

build:
	go build -o docker-control-server ./cmd/server

test:
	go test ./...

test-cover:
	go test -cover ./...

proto:
	protoc --go_out=. --go-grpc_out=. proto/docker_control.proto

clean:
	rm -f docker-control-server

tidy:
	go mod tidy

run: build
	./docker-control-server

run-debug: build
	./docker-control-server -log-level debug