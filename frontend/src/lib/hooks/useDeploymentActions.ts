import { useState } from 'react';
import { useMutation } from '@apollo/client';
import { toast } from 'sonner';
import { 
  GET_DEPLOYMENTS, 
  STOP_DEPLOYMENT,
  START_DEPLOYMENT,
  DELETE_DEPLOYMENT,
  type Deployment 
} from '@/lib/graphql';

export function useDeploymentActions() {
  const [stopLoading, setStopLoading] = useState<Record<string, boolean>>({});
  const [startLoading, setStartLoading] = useState<Record<string, boolean>>({});
  const [deleteLoading, setDeleteLoading] = useState<Record<string, boolean>>({});

  const [stopDeployment] = useMutation<{ stopDeployment: boolean }>(STOP_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const [startDeployment] = useMutation<{ startDeployment: Deployment }>(START_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const [deleteDeployment] = useMutation<{ deleteDeployment: boolean }>(DELETE_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const handleStopDeployment = async (
    id: string, 
    onStatusUpdate: (id: string, status: any) => void,
    onRefetch: (id: string) => void
  ) => {
    setStopLoading(prev => ({ ...prev, [id]: true }));
    try {
      await stopDeployment({ variables: { id } });
      toast.success('Deployment stopped successfully!');
      onStatusUpdate(id, { state: 'stopping' });
      setTimeout(() => onRefetch(id), 2000);
    } catch (error) {
      console.error('Stop deployment error:', error);
      toast.error('Failed to stop deployment');
    } finally {
      setStopLoading(prev => {
        const { [id]: _removed, ...rest } = prev;
        return rest;
      });
    }
  };

  const handleStartDeployment = async (
    deployment: Deployment,
    onStatusUpdate: (id: string, status: any) => void,
    onRefetch: (id: string) => void
  ) => {   
    setStartLoading(prev => ({ ...prev, [deployment.id]: true }));
    try {
      await startDeployment({ 
        variables: { id: deployment.id } 
      });
      toast.success('Deployment restarted successfully!');
      onStatusUpdate(deployment.id, { state: 'starting' });
      setTimeout(() => onRefetch(deployment.id), 2000);
    } catch (error) {
      console.error('Start deployment error:', error);
      toast.error('Failed to restart deployment');
    } finally {
      setStartLoading(prev => {
        const { [deployment.id]: _removed, ...rest } = prev;
        return rest;
      });
    }
  };

  const handleDeleteDeployment = async (
    id: string,
    onStatusRemove: (id: string) => void
  ) => {
    setDeleteLoading(prev => ({ ...prev, [id]: true }));
    try {
      await deleteDeployment({ variables: { id } });
      toast.success('Deployment deleted successfully!');
      onStatusRemove(id);
    } catch (error) {
      console.error('Delete deployment error:', error);
      toast.error('Failed to delete deployment');
    } finally {
      setDeleteLoading(prev => {
        const { [id]: _removed, ...rest } = prev;
        return rest;
      });
    }
  };

  return {
    stopLoading,
    startLoading,
    deleteLoading,
    handleStopDeployment,
    handleStartDeployment,
    handleDeleteDeployment
  };
} 