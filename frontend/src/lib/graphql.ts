import { gql } from '@apollo/client';

// Auth Mutations
export const REGISTER_MUTATION = gql`
  mutation Register($input: CreateAccountInput!) {
    register(input: $input) {
      token
    }
  }
`;

export const LOGIN_MUTATION = gql`
  mutation Login($email: String!, $password: String!) {
    login(email: $email, password: $password) {
      token
    }
  }
`;

// Deployment Queries
export const GET_DEPLOYMENTS = gql`
  query GetDeployments {
    deployments {
      id
      composeYaml
      createdAt
    }
  }
`;

export const GET_DEPLOYMENT_STATUS = gql`
  query GetDeploymentStatus($id: ID!) {
    deploymentStatus(id: $id) {
      uuid
      state
      message
      status
      containers {
        name
        state
        health
        ports
      }
    }
  }
`;

// Deployment Mutations
// Create a new deployment from a docker-compose definition
export const CREATE_DEPLOYMENT = gql`
  mutation CreateDeployment($composeYaml: String!) {
    createDeployment(composeYaml: $composeYaml) {
      id
      composeYaml
      createdAt
    }
  }
`;

// Start / restart a deployment by its UUID
export const START_DEPLOYMENT = gql`
  mutation StartDeployment($id: ID!) {
    startDeployment(id: $id) {
      id
      composeYaml
      createdAt
    }
  }
`;

export const STOP_DEPLOYMENT = gql`
  mutation StopDeployment($id: ID!) {
    stopDeployment(id: $id)
  }
`;

// TypeScript types based on backend schema
export interface AuthPayload {
  token: string;
}

export interface CreateAccountInput {
  email: string;
  password: string;
}

export interface Deployment {
  id: string;
  composeYaml: string;
  createdAt: string;
}

export interface ContainerStatus {
  name: string;
  state: string;
  health: string | null;
  ports: string[];
}

export interface DeploymentStatus {
  uuid: string;
  state: string;
  message: string | null;
  status: number;
  containers: ContainerStatus[];
} 