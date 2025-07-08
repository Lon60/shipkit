.PHONY: build test clean proto help run run-debug dev install-grpcui

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
	@echo "  dev        - Build server, launch it and grpcui for local development"
	@echo "  install-grpcui - Install grpcui"

build:
	go build -o docker-control-server ./cmd/server

test:
	go test ./...

test-cover:
	go test -cover ./...

proto:
	@# Ensure protoc-gen-go & protoc-gen-go-grpc are in PATH (install if missing)
	@command -v protoc-gen-go >/dev/null 2>&1 || go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
	@command -v protoc-gen-go-grpc >/dev/null 2>&1 || go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
	PATH=$$PATH:$(shell go env GOPATH)/bin \
	protoc --go_out=paths=source_relative:. --go-grpc_out=paths=source_relative:. proto/docker_control.proto

clean:
	rm -f docker-control-server

tidy:
	go mod tidy

run: build
	./docker-control-server

run-debug: build
	./docker-control-server -log-level debug

PORT ?= 50051

GRPCUI_BIN := $(shell [ -n "$(GOBIN)" ] && echo $(GOBIN) || echo $(shell go env GOPATH)/bin)/grpcui

install-grpcui:
	@if [ ! -x "$(GRPCUI_BIN)" ]; then \
		echo "Installing grpcui..."; \
		go install github.com/fullstorydev/grpcui/cmd/grpcui@latest; \
	fi

dev: build install-grpcui
	./docker-control-server & \
	SERVER_PID=$$!; \
	echo "Server PID $$SERVER_PID"; \
	sleep 1; \
	$(GRPCUI_BIN) -plaintext localhost:$(PORT); \
	kill $$SERVER_PID