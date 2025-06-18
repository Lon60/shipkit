import { gql } from '@apollo/client';

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

export const GET_DEPLOYMENTS = gql`
  query GetDeployments {
    deployments {
      id
      name
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

export const CREATE_DEPLOYMENT = gql`
  mutation CreateDeployment($input: CreateDeploymentDTO!) {
    createDeployment(input: $input) {
      id
      name
      composeYaml
      createdAt
    }
  }
`;

export const UPDATE_DEPLOYMENT = gql`
  mutation UpdateDeployment($id: ID!, $input: UpdateDeploymentDTO!) {
    updateDeployment(id: $id, input: $input) {
      id
      name
      composeYaml
      createdAt
    }
  }
`;

export const START_DEPLOYMENT = gql`
  mutation StartDeployment($id: ID!) {
    startDeployment(id: $id) {
      id
      name
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

export const DELETE_DEPLOYMENT = gql`
  mutation DeleteDeployment($id: ID!) {
    deleteDeployment(id: $id)
  }
`;

export const CHANGE_PASSWORD = gql`
  mutation ChangePassword($input: ChangePasswordInput!) {
    changePassword(input: $input) {
      token
    }
  }
`;

export const GET_STATUS = gql`
  query GetStatus {
    status {
      status
      adminInitialized
    }
  }
`;

export interface AuthPayload {
  token: string;
}

export interface CreateAccountInput {
  email: string;
  password: string;
}

export interface CreateDeploymentDTO {
  name: string;
  composeYaml: string;
}

export interface UpdateDeploymentDTO {
  name?: string;
  composeYaml?: string;
}

export interface ChangePasswordInput {
  oldPassword: string;
  newPassword: string;
}

export interface Deployment {
  id: string;
  name: string;
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

export interface Status {
  status: string;
  adminInitialized: boolean;
} 