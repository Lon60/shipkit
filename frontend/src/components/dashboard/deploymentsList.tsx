'use client';

import { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useLazyQuery } from '@apollo/client';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogHeader, 
  DialogTitle, 
  DialogTrigger 
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { 
  GET_DEPLOYMENTS, 
  GET_DEPLOYMENT_STATUS, 
  STOP_DEPLOYMENT,
  START_DEPLOYMENT,
  DELETE_DEPLOYMENT,
  type Deployment, 
  type DeploymentStatus 
} from '@/lib/graphql';
import { Square, Eye, Play, Loader2, Trash2, AlertTriangle } from 'lucide-react';

export function DeploymentsList() {
  const [selectedDeployment, setSelectedDeployment] = useState<string | null>(null);
  const [deploymentStatuses, setDeploymentStatuses] = useState<Record<string, DeploymentStatus>>({});
  const [stopLoading, setStopLoading] = useState<Record<string, boolean>>({});
  const [startLoading, setStartLoading] = useState<Record<string, boolean>>({});
  const [deleteLoading, setDeleteLoading] = useState<Record<string, boolean>>({});
  const [currentFetchingId, setCurrentFetchingId] = useState<string | null>(null);
  const [deleteConfirmation, setDeleteConfirmation] = useState<{
    isOpen: boolean;
    deployment: Deployment | null;
    confirmText: string;
  }>({
    isOpen: false,
    deployment: null,
    confirmText: '',
  });
  
  const { data: deploymentsData, loading: deploymentsLoading } = useQuery<{
    deployments: Deployment[];
  }>(GET_DEPLOYMENTS);

  const { data: detailStatusData, loading: detailStatusLoading } = useQuery<{
    deploymentStatus: DeploymentStatus;
  }>(GET_DEPLOYMENT_STATUS, {
    variables: { id: selectedDeployment },
    skip: !selectedDeployment,
    pollInterval: 5000,
    fetchPolicy: 'cache-and-network'
  });

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

  useEffect(() => {
    if (deploymentsData?.deployments) {
      deploymentsData.deployments.forEach(deployment => {
        void fetchDeploymentStatus(deployment.id);
      });
    }
  }, [deploymentsData?.deployments, fetchDeploymentStatus]);

  useEffect(() => {
    if (!deploymentsData?.deployments?.length) return;

    const interval = setInterval(() => {
      deploymentsData.deployments.forEach(deployment => {
        void fetchDeploymentStatus(deployment.id);
      });
    }, 15000);

    return () => clearInterval(interval);
  }, [deploymentsData?.deployments, fetchDeploymentStatus]);

  const [stopDeployment] = useMutation<{ stopDeployment: boolean }>(STOP_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const [startDeployment] = useMutation<{ startDeployment: Deployment }>(START_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const [deleteDeployment] = useMutation<{ deleteDeployment: boolean }>(DELETE_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const handleStopDeployment = async (id: string) => {
    setStopLoading(prev => ({ ...prev, [id]: true }));
    try {
      await stopDeployment({ variables: { id } });
      toast.success('Deployment stopped successfully!');
      setDeploymentStatuses(prev => ({
        ...prev,
        [id]: { 
          uuid: prev[id]?.uuid ?? id,
          state: 'stopping',
          message: prev[id]?.message ?? null,
          status: prev[id]?.status ?? 0,
          containers: prev[id]?.containers ?? []
        }
      }));
      setTimeout(() => void fetchDeploymentStatus(id), 2000);
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

  const handleStartDeployment = async (deployment: Deployment) => {   
    setStartLoading(prev => ({ ...prev, [deployment.id]: true }));
    try {
      await startDeployment({ 
        variables: { id: deployment.id } 
      });
      toast.success('Deployment restarted successfully!');
      setDeploymentStatuses(prev => ({
        ...prev,
        [deployment.id]: { 
          uuid: prev[deployment.id]?.uuid ?? deployment.id,
          state: 'starting',
          message: prev[deployment.id]?.message ?? null,
          status: prev[deployment.id]?.status ?? 0,
          containers: prev[deployment.id]?.containers ?? []
        }
      }));
      setTimeout(() => void fetchDeploymentStatus(deployment.id), 2000);
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

  const openDeleteConfirmation = (deployment: Deployment) => {
    setDeleteConfirmation({
      isOpen: true,
      deployment,
      confirmText: '',
    });
  };

  const closeDeleteConfirmation = () => {
    setDeleteConfirmation({
      isOpen: false,
      deployment: null,
      confirmText: '',
    });
  };

  const handleDeleteDeployment = async () => {
    const { deployment } = deleteConfirmation;
    if (!deployment) return;

    setDeleteLoading(prev => ({ ...prev, [deployment.id]: true }));
    try {
      await deleteDeployment({ variables: { id: deployment.id } });
      toast.success('Deployment deleted successfully!');
      setDeploymentStatuses(prev => {
        const { [deployment.id]: _removed, ...rest } = prev;
        return rest;
      });
      closeDeleteConfirmation();
    } catch (error) {
      console.error('Delete deployment error:', error);
      toast.error('Failed to delete deployment');
    } finally {
      setDeleteLoading(prev => {
        const { [deployment.id]: _removed, ...rest } = prev;
        return rest;
      });
    }
  };

  const getDeploymentStatus = (deploymentId: string): string => {
    return deploymentStatuses[deploymentId]?.state ?? 'unknown';
  };

  const isDeploymentStopped = (deploymentId: string): boolean => {
    const status = getDeploymentStatus(deploymentId).toLowerCase();
    return status === 'stopped' || status === 'unknown';
  };

  const getStatusBadgeColor = (state: string) => {
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
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
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
          const isStopped = isDeploymentStopped(deployment.id);
          const status = getDeploymentStatus(deployment.id);
          
          return (
            <Card key={deployment.id} className="border-border bg-card">
              <CardHeader>
                <div className="flex justify-between items-start">
                  <div>
                    <CardTitle className="text-base text-foreground">
                      {deployment.name}
                    </CardTitle>
                    <CardDescription className="text-muted-foreground">
                      ID: {deployment.id}
                      <br />
                      Created: {formatDate(deployment.createdAt)}
                      {status !== 'unknown' && (
                        <span className="ml-2">
                          <Badge className={getStatusBadgeColor(status)}>
                            {status}
                          </Badge>
                        </span>
                      )}
                    </CardDescription>
                  </div>
                  <div className="flex space-x-2">
                    <Dialog>
                      <DialogTrigger asChild>
                        <Button 
                          variant="outline" 
                          size="sm"
                          onClick={() => setSelectedDeployment(deployment.id)}
                        >
                          <Eye className="h-4 w-4 mr-2" />
                          View
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
                        <DialogHeader>
                          <DialogTitle>{deployment.name}</DialogTitle>
                          <DialogDescription>
                            ID: {deployment.id}
                          </DialogDescription>
                        </DialogHeader>
                        
                        <Tabs defaultValue="compose" className="w-full">
                          <TabsList>
                            <TabsTrigger value="compose">Docker Compose</TabsTrigger>
                            <TabsTrigger value="status">Status</TabsTrigger>
                          </TabsList>
                          
                          <TabsContent value="compose" className="space-y-4">
                            <div>
                              <h4 className="font-medium mb-2">Docker Compose YAML</h4>
                              <pre className="bg-muted/50 p-4 rounded-lg text-sm overflow-x-auto border border-border">
                                {deployment.composeYaml}
                              </pre>
                            </div>
                          </TabsContent>
                          
                          <TabsContent value="status" className="space-y-4">
                            {detailStatusLoading ? (
                              <div className="flex justify-center py-4">
                                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary"></div>
                              </div>
                            ) : detailStatusData?.deploymentStatus ? (
                              <div className="space-y-4">
                                <div className="grid grid-cols-2 gap-4">
                                  <div>
                                    <p className="text-sm font-medium">Status</p>
                                    <Badge className={getStatusBadgeColor(detailStatusData.deploymentStatus.state)}>
                                      {detailStatusData.deploymentStatus.state}
                                    </Badge>
                                  </div>
                                  <div>
                                    <p className="text-sm font-medium">Message</p>
                                    <p className="text-sm text-muted-foreground">{detailStatusData.deploymentStatus.message}</p>
                                  </div>
                                </div>
                                
                                {detailStatusData.deploymentStatus.containers.length > 0 && (
                                  <div>
                                    <h4 className="font-medium mb-2">Containers</h4>
                                    <div className="space-y-2">
                                      {detailStatusData.deploymentStatus.containers.map((container, index) => (
                                        <div key={index} className="border rounded-lg p-3 border-border">
                                          <div className="flex justify-between items-start">
                                            <div>
                                              <p className="font-medium">{container.name}</p>
                                              <div className="flex space-x-4 mt-1">
                                                <span className="text-sm">State: {container.state}</span>
                                                <span className="text-sm">Health: {container.health}</span>
                                              </div>
                                            </div>
                                            {container.ports.length > 0 && (
                                              <div className="text-right">
                                                <p className="text-sm font-medium">Ports</p>
                                                {container.ports.map((port, portIndex) => (
                                                  <p key={portIndex} className="text-sm text-muted-foreground">{port}</p>
                                                ))}
                                              </div>
                                            )}
                                          </div>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}
                              </div>
                            ) : (
                              <p className="text-muted-foreground">No status information available</p>
                            )}
                          </TabsContent>
                        </Tabs>
                      </DialogContent>
                    </Dialog>
                    
                    {isStopped ? (
                      <Button 
                        variant="default" 
                        size="sm"
                        disabled={!!startLoading[deployment.id] || getDeploymentStatus(deployment.id).toLowerCase() === 'starting'}
                        onClick={() => handleStartDeployment(deployment)}
                      >
                        {startLoading[deployment.id] ? (
                          <>
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                            Starting
                          </>
                        ) : (
                          <>
                            <Play className="h-4 w-4 mr-2" />
                            Start
                          </>
                        )}
                      </Button>
                    ) : (
                      <Button 
                        variant="destructive" 
                        size="sm"
                        disabled={!!stopLoading[deployment.id] || getDeploymentStatus(deployment.id).toLowerCase() === 'stopping'}
                        onClick={() => handleStopDeployment(deployment.id)}
                      >
                        {stopLoading[deployment.id] ? (
                          <>
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                            Stopping
                          </>
                        ) : (
                          <>
                            <Square className="h-4 w-4 mr-2" />
                            Stop
                          </>
                        )}
                      </Button>
                    )}
                    
                    <Button 
                      variant="outline" 
                      size="sm"
                      disabled={!!deleteLoading[deployment.id]}
                      onClick={() => openDeleteConfirmation(deployment)}
                    >
                      {deleteLoading[deployment.id] ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Deleting
                        </>
                      ) : (
                        <>
                          <Trash2 className="h-4 w-4 mr-2" />
                          Delete
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              </CardHeader>
            </Card>
          );
        })}
      </div>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteConfirmation.isOpen} onOpenChange={closeDeleteConfirmation}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-destructive">
              <AlertTriangle className="h-5 w-5" />
              Delete Deployment
            </DialogTitle>
            <DialogDescription>
              This action cannot be undone. This will permanently delete the deployment and all associated data.
            </DialogDescription>
          </DialogHeader>
          
          {deleteConfirmation.deployment && (
            <div className="space-y-4">
              <div>
                <p className="text-sm font-medium mb-2">
                  Please type <span className="font-mono bg-muted px-1 rounded text-destructive">{deleteConfirmation.deployment.name}</span> to confirm:
                </p>
                <Input
                  value={deleteConfirmation.confirmText}
                  onChange={(e) => setDeleteConfirmation(prev => ({ ...prev, confirmText: e.target.value }))}
                  placeholder="Enter deployment name"
                  className="font-mono"
                />
              </div>
              
              <div className="flex justify-end space-x-2">
                <Button variant="outline" onClick={closeDeleteConfirmation}>
                  Cancel
                </Button>
                <Button 
                  variant="destructive" 
                  disabled={
                    deleteConfirmation.confirmText !== deleteConfirmation.deployment.name ||
                    !!deleteLoading[deleteConfirmation.deployment.id]
                  }
                  onClick={handleDeleteDeployment}
                >
                  {deleteLoading[deleteConfirmation.deployment.id] ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Deleting...
                    </>
                  ) : (
                    'Delete Deployment'
                  )}
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}