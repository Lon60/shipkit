'use client';

import { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useLazyQuery } from '@apollo/client';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
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
  type Deployment, 
  type DeploymentStatus 
} from '@/lib/graphql';
import { Square, Eye, Play, Loader2 } from 'lucide-react';

export function DeploymentsList() {
  const [selectedDeployment, setSelectedDeployment] = useState<string | null>(null);
  const [deploymentStatuses, setDeploymentStatuses] = useState<Record<string, DeploymentStatus>>({});
  const [stopLoading, setStopLoading] = useState<Record<string, boolean>>({});
  const [startLoading, setStartLoading] = useState<Record<string, boolean>>({});
  const [currentFetchingId, setCurrentFetchingId] = useState<string | null>(null);
  
  const { data: deploymentsData, loading: deploymentsLoading } = useQuery<{
    deployments: Deployment[];
  }>(GET_DEPLOYMENTS);

  // Separate query for the detailed view - this won't interfere with main list
  const { data: detailStatusData, loading: detailStatusLoading } = useQuery<{
    deploymentStatus: DeploymentStatus;
  }>(GET_DEPLOYMENT_STATUS, {
    variables: { id: selectedDeployment },
    skip: !selectedDeployment,
    pollInterval: 5000, // Poll every 5 seconds for detailed view
    fetchPolicy: 'cache-and-network' // Don't interfere with main list cache
  });

  // Lazy query for fetching individual deployment statuses for the main list
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

  // Fetch status for all deployments initially
  useEffect(() => {
    if (deploymentsData?.deployments) {
      deploymentsData.deployments.forEach(deployment => {
        // Always fetch fresh status for each deployment
        void fetchDeploymentStatus(deployment.id);
      });
    }
  }, [deploymentsData?.deployments, fetchDeploymentStatus]);

  // Poll all deployment statuses every 15 seconds (less frequent to avoid conflicts)
  useEffect(() => {
    if (!deploymentsData?.deployments?.length) return;

    const interval = setInterval(() => {
      deploymentsData.deployments.forEach(deployment => {
        void fetchDeploymentStatus(deployment.id);
      });
    }, 15000); // Poll every 15 seconds

    return () => clearInterval(interval);
  }, [deploymentsData?.deployments, fetchDeploymentStatus]);

  const [stopDeployment] = useMutation<{ stopDeployment: boolean }>(STOP_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const [startDeployment] = useMutation<{ startDeployment: Deployment }>(START_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const handleStopDeployment = async (id: string) => {
    // Mark as loading
    setStopLoading(prev => ({ ...prev, [id]: true }));
    try {
      await stopDeployment({ variables: { id } });
      toast.success('Deployment stopped successfully!');
      // Update local status immediately
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
      // Fetch fresh status after a short delay
      setTimeout(() => void fetchDeploymentStatus(id), 2000);
    } catch (error) {
      console.error('Stop deployment error:', error);
      toast.error('Failed to stop deployment');
    } finally {
      // Clear loading state
      setStopLoading(prev => {
        const { [id]: _removed, ...rest } = prev;
        return rest;
      });
    }
  };

  const handleStartDeployment = async (deployment: Deployment) => {
    // Mark as loading
    setStartLoading(prev => ({ ...prev, [deployment.id]: true }));
    try {
      await startDeployment({ 
        variables: { id: deployment.id } 
      });
      toast.success('Deployment restarted successfully!');
      // Update local status immediately
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
      // Fetch fresh status after a short delay
      setTimeout(() => void fetchDeploymentStatus(deployment.id), 2000);
    } catch (error) {
      console.error('Start deployment error:', error);
      toast.error('Failed to restart deployment');
    } finally {
      // Clear loading state
      setStartLoading(prev => {
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
                    <CardTitle className="text-sm font-mono text-foreground">
                      {deployment.id}
                    </CardTitle>
                    <CardDescription className="text-muted-foreground">
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
                          <DialogTitle>Deployment Details</DialogTitle>
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
                  </div>
                </div>
              </CardHeader>
            </Card>
          );
        })}
      </div>
    </div>
  );
} 