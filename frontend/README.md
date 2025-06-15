# Shipkit Frontend

A modern, responsive frontend for the Shipkit Platform as a Service (PaaS) - an alternative to Coolify and similar platforms. Built with Next.js 15, React 19, TypeScript, and Tailwind CSS.

## Prerequisites

- Node.js 18+ or Bun
- Spring Boot backend (gateway-api) running on port 8080
- Docker and Docker Compose (for the backend microservices)

## Getting Started

### 1. Clone and Install Dependencies

```bash
cd frontend

bun install
```

### 2. Environment Setup

Copy the environment example and configure:

```bash
cp .env.example .env.local
```

Update `.env.local` with your configuration:

```env
# GraphQL endpoint (backend API)
NEXT_PUBLIC_GRAPHQL_URL=http://localhost:8080/graphql

# Application name
NEXT_PUBLIC_APP_NAME=Shipkit
```

### 3. Start the Development Server

```bash
bun dev
```

The application will be available at `http://localhost:3000`.
