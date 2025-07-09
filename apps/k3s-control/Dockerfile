# syntax=docker/dockerfile:1

# --- Builder stage ---
FROM golang:1.21-alpine AS builder

RUN apk add --no-cache git ca-certificates tzdata
WORKDIR /src

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags='-w -s' \
    -o k3s-control-server ./cmd/server

# --- Runtime stage ---
FROM gcr.io/distroless/static AS runtime
COPY --from=builder /src/k3s-control-server /usr/bin/k3s-control-server

EXPOSE 9998

ENTRYPOINT ["/usr/bin/k3s-control-server"] 