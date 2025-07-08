'use client';

import { useState, useEffect } from 'react';
import { useQuery } from '@apollo/client';
import { GET_DEPLOYMENTS, type Deployment } from '@/lib/graphql';
import { useDeploymentStatus } from '@/lib/hooks/useDeploymentStatus';
import { useDeploymentActions } from '@/lib/hooks/useDeploymentActions';
import { DeploymentCard } from './DeploymentCard';
import { DeploymentDetailsDialog } from './DeploymentDetailsDialog';
import { DeleteConfirmationDialog } from './DeleteConfirmationDialog';
import { EditDeploymentDialog } from './EditDeploymentDialog';

export function DeploymentsList() {
  const [selectedDeployment, setSelectedDeployment] = useState<Deployment | null>(null);
  const [editDeployment, setEditDeployment] = useState<Deployment | null>(null);
  const [deleteConfirmation, setDeleteConfirmation] = useState<{
    isOpen: boolean;
    deployment: Deployment | null;
  }>({
    isOpen: false,
    deployment: null,
  });
  
  const { data: deploymentsData, loading: deploymentsLoading } = useQuery<{
    deployments: Deployment[];
  }>(GET_DEPLOYMENTS);

  const {
    getDeploymentStatus,
    isDeploymentStopped,
    getStatusBadgeColor,
    updateDeploymentStatus,
    removeDeploymentStatus,
    setupPolling,
    fetchDeploymentStatus
  } = useDeploymentStatus();

  const {
    stopLoading,
    startLoading,
    deleteLoading,
    handleStopDeployment,
    handleStartDeployment,
    handleDeleteDeployment
  } = useDeploymentActions();

  useEffect(() => {
    if (deploymentsData?.deployments?.length) {
      return setupPolling(deploymentsData.deployments);
    }
  }, [deploymentsData?.deployments, setupPolling]);

  const openDeleteConfirmation = (deployment: Deployment) => {
    setDeleteConfirmation({
      isOpen: true,
      deployment,
    });
  };

  const closeDeleteConfirmation = () => {
    setDeleteConfirmation({
      isOpen: false,
      deployment: null,
    });
  };

  const openEditDialog = (deployment: Deployment) => {
    setEditDeployment(deployment);
  };

  const closeEditDialog = () => {
    setEditDeployment(null);
  };

  const refetchStatus = (id: string) => {
    void fetchDeploymentStatus(id);
  };

  const handleDeploymentStart = (deployment: Deployment) => {
    void handleStartDeployment(
      deployment,
      updateDeploymentStatus,
      refetchStatus
    );
  };

  const handleDeploymentStop = (id: string) => {
    void handleStopDeployment(
      id,
      updateDeploymentStatus,
      refetchStatus
    );
  };

  const handleDeploymentDelete = () => {
    if (!deleteConfirmation.deployment) return;
    
    void handleDeleteDeployment(
      deleteConfirmation.deployment.id,
      removeDeploymentStatus
    );
    closeDeleteConfirmation();
  };

  if (deploymentsLoading) {
    return (
      <div className="flex justify-center items-center h-32">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  const deployments = deploymentsData?.deployments ?? [];

  if (deployments.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <h3 className="text-lg font-medium text-foreground mb-2">No deployments yet</h3>
        <p className="text-muted-foreground max-w-sm">
          Start by creating your first deployment using the Deploy button above.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-foreground">Deployments</h2>

      <div className="grid gap-4">
        {deployments.map((deployment) => {
          const status = getDeploymentStatus(deployment.id);
          const isStopped = isDeploymentStopped(deployment.id);
          
          return (
            <DeploymentCard
              key={deployment.id}
              deployment={deployment}
              status={status}
              isStopped={isStopped}
              getStatusBadgeColor={getStatusBadgeColor}
              stopLoading={!!stopLoading[deployment.id]}
              startLoading={!!startLoading[deployment.id]}
              deleteLoading={!!deleteLoading[deployment.id]}
              onView={() => setSelectedDeployment(deployment)}
              onEdit={() => openEditDialog(deployment)}
              onStart={() => handleDeploymentStart(deployment)}
              onStop={() => handleDeploymentStop(deployment.id)}
              onDelete={() => openDeleteConfirmation(deployment)}
            />
          );
        })}
      </div>

      <DeploymentDetailsDialog
        deployment={selectedDeployment}
        isOpen={!!selectedDeployment}
        onClose={() => setSelectedDeployment(null)}
        getStatusBadgeColor={getStatusBadgeColor}
      />

      <EditDeploymentDialog
        deployment={editDeployment}
        isOpen={!!editDeployment}
        onClose={closeEditDialog}
      />

      <DeleteConfirmationDialog
        isOpen={deleteConfirmation.isOpen}
        deployment={deleteConfirmation.deployment}
        deleteLoading={deleteConfirmation.deployment ? !!deleteLoading[deleteConfirmation.deployment.id] : false}
        onClose={closeDeleteConfirmation}
        onConfirm={handleDeploymentDelete}
      />
    </div>
  );
}