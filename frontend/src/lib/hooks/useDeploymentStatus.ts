import { useState, useCallback } from 'react';
import { useLazyQuery } from '@apollo/client';
import { GET_DEPLOYMENT_STATUS, type DeploymentStatus, type Deployment } from '@/lib/graphql';

export function useDeploymentStatus() {
  const [deploymentStatuses, setDeploymentStatuses] = useState<Record<string, DeploymentStatus>>({});
  const [currentFetchingId, setCurrentFetchingId] = useState<string | null>(null);

  const [fetchStatusForList] = useLazyQuery<{ deploymentStatus: DeploymentStatus }>(GET_DEPLOYMENT_STATUS, {
    fetchPolicy: 'network-only',
    onCompleted: (data) => {
      if (data?.deploymentStatus && currentFetchingId) {
        setDeploymentStatuses(prev => ({
          ...prev,
          [currentFetchingId]: data.deploymentStatus
        }));
        setCurrentFetchingId(null);
      }
    },
    onError: (error) => {
      console.error('Failed to fetch deployment status:', error);
      setCurrentFetchingId(null);
    }
  });

  const fetchDeploymentStatus = useCallback((deploymentId: string) => {
    setCurrentFetchingId(deploymentId);
    void fetchStatusForList({ variables: { id: deploymentId } });
  }, [fetchStatusForList]);

  const getDeploymentStatus = useCallback((deploymentId: string): string => {
    return deploymentStatuses[deploymentId]?.state ?? 'unknown';
  }, [deploymentStatuses]);

  const isDeploymentStopped = useCallback((deploymentId: string): boolean => {
    const status = getDeploymentStatus(deploymentId).toLowerCase();
    return status === 'stopped' || status === 'unknown';
  }, [getDeploymentStatus]);

  const getStatusBadgeColor = useCallback((state: string) => {
    switch (state.toLowerCase()) {
      case 'running':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case 'stopped':
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
      case 'starting':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200';
      case 'stopping':
        return 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200';
    }
  }, []);

  const updateDeploymentStatus = useCallback((deploymentId: string, newStatus: Partial<DeploymentStatus>) => {
    setDeploymentStatuses(prev => ({
      ...prev,
      [deploymentId]: {
        uuid: prev[deploymentId]?.uuid ?? deploymentId,
        state: newStatus.state ?? prev[deploymentId]?.state ?? 'unknown',
        message: newStatus.message ?? prev[deploymentId]?.message ?? null,
        status: newStatus.status ?? prev[deploymentId]?.status ?? 0,
        containers: newStatus.containers ?? prev[deploymentId]?.containers ?? []
      }
    }));
  }, []);

  const removeDeploymentStatus = useCallback((deploymentId: string) => {
    setDeploymentStatuses(prev => {
      const { [deploymentId]: _removed, ...rest } = prev;
      return rest;
    });
  }, []);

  const setupPolling = useCallback((deployments: Deployment[]) => {
    deployments.forEach(deployment => {
      void fetchDeploymentStatus(deployment.id);
    });

    const interval = setInterval(() => {
      deployments.forEach(deployment => {
        void fetchDeploymentStatus(deployment.id);
      });
    }, 15000);

    return () => clearInterval(interval);
  }, [fetchDeploymentStatus]);

  return {
    deploymentStatuses,
    fetchDeploymentStatus,
    getDeploymentStatus,
    isDeploymentStopped,
    getStatusBadgeColor,
    updateDeploymentStatus,
    removeDeploymentStatus,
    setupPolling
  };
} 