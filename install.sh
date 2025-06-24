#!/bin/bash
## Shipkit Installer
##
## This script will install Shipkit on your system.
## You can customize the installation by setting the following environment variables before running the script:
##
## - DOCKER_CONTROL_PORT: Port for the Docker Control service (default: 9998)
## - DOCKER_CONTROL_LOG_LEVEL: Log level for the Docker Control service (default: info)
## - DATABASE_USERNAME: PostgreSQL database username (default: shipkit)
## - DATABASE_PASSWORD: PostgreSQL database password (default: random)
## - JWT_SECRET: Secret for signing JSON Web Tokens (default: random)
## - JWT_EXPIRATION_MS: JWT expiration time in milliseconds (default: 86400000)
## - CORS_ALLOWED_ORIGINS: Allowed origins for CORS (default: http://localhost,http://localhost:3000,http://127.0.0.1,http://127.0.0.1:3000)
## - NEXT_PUBLIC_APP_NAME: The public name of the application (default: Shipkit)
## - DOCKER_SOCKET_PATH: Path to the Docker socket (default: /var/run/docker.sock)
## - INSTALL_DIR: The directory to install Shipkit into (default: $HOME/shipkit)

# Stop on errors
set -e
set -o pipefail

# --- Configuration ---
# Use environment variables if they are set, otherwise use defaults.
DOCKER_CONTROL_PORT=${DOCKER_CONTROL_PORT:-9998}
DOCKER_CONTROL_LOG_LEVEL=${DOCKER_CONTROL_LOG_LEVEL:-info}
DATABASE_USERNAME=${DATABASE_USERNAME:-shipkit}
DATABASE_PASSWORD=${DATABASE_PASSWORD:-$(openssl rand -base64 32 | tr -d 'iIloO0+/')}
JWT_SECRET=${JWT_SECRET:-$(openssl rand -base64 48 | tr -d 'iIloO0+/')}
JWT_EXPIRATION_MS=${JWT_EXPIRATION_MS:-86400000}
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-"http://localhost,http://localhost:3000,http://127.0.0.1,http://127.0.0.1:3000"}
NEXT_PUBLIC_APP_NAME=${NEXT_PUBLIC_APP_NAME:-Shipkit}
DOCKER_SOCKET_PATH=${DOCKER_SOCKET_PATH:-/var/run/docker.sock}
INSTALL_DIR=${INSTALL_DIR:-"$HOME/shipkit"}
REPO_URL="https://raw.githubusercontent.com/lon60/shipkit/main"


# Function to detect docker-compose command
detect_compose_command() {
  if command -v docker-compose &> /dev/null; then
    COMPOSER="docker-compose"
  elif docker compose version &> /dev/null; then
    COMPOSER="docker compose"
  else
    echo "Error: Neither docker-compose nor 'docker compose' was found. Please install one of them to continue." >&2
    exit 1
  fi
}

# --- Main Script ---

# Welcome message
echo "Welcome to the Shipkit installer!"
echo "This script will install and configure Shipkit on your system."
echo ""

# Root check
if [ "$EUID" -ne 0 ]; then
  echo "Please run this script as root or with sudo"
  exit
fi

# Check for dependencies
echo "Checking for dependencies..."
if ! [ -x "$(command -v docker)" ]; then
  echo "Error: Docker is not installed. Please install Docker and try again." >&2
  exit 1
fi

detect_compose_command

echo "Dependencies are satisfied."
echo ""

# Create installation directory
mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

# Create .env file
echo "Creating .env file with generated secrets..."
cat > .env <<EOL
# Shipkit Environment Variables
DOCKER_CONTROL_PORT=${DOCKER_CONTROL_PORT}
DOCKER_CONTROL_LOG_LEVEL=${DOCKER_CONTROL_LOG_LEVEL}
DATABASE_USERNAME=${DATABASE_USERNAME}
DATABASE_PASSWORD=${DATABASE_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION_MS=${JWT_EXPIRATION_MS}
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
NEXT_PUBLIC_APP_NAME=${NEXT_PUBLIC_APP_NAME}
DOCKER_SOCKET_PATH=${DOCKER_SOCKET_PATH}
EOL
echo ".env file created."
echo ""

# Download docker-compose.yml and nginx files from GitHub
echo "Downloading configuration files..."
curl -sSL -o docker-compose.yml "$REPO_URL/docker-compose.yml"
mkdir -p scripts/nginx/snippets
curl -sSL -o scripts/nginx/shipkit-default.conf "$REPO_URL/scripts/nginx/shipkit-default.conf"
curl -sSL -o scripts/nginx/snippets/cors.conf "$REPO_URL/scripts/nginx/snippets/cors.conf"
curl -sSL -o scripts/nginx/snippets/proxy-headers.conf "$REPO_URL/scripts/nginx/snippets/proxy-headers.conf"
echo "Configuration files downloaded."
echo ""

# Start Shipkit
echo "Starting Shipkit... (this may take a few minutes for the first run)"
$COMPOSER up -d

# Getting server IP
SERVER_IP=$(curl -s http://checkip.amazonaws.com || printf "localhost")

echo ""
echo "----------------------------------------"
echo "Shipkit has been installed successfully!"
echo "You should be able to access the dashboard at http://$SERVER_IP"
echo ""
echo "Important: Please ensure that your firewall allows traffic on ports 80 and 443."
echo ""
echo "You can manage your installation by navigating to the '$INSTALL_DIR' directory and using docker-compose commands."
echo "For example, to stop Shipkit, run: cd $INSTALL_DIR && $COMPOSER down"
echo "----------------------------------------" 