import { useQuery } from '@apollo/client';
import { Badge } from '@/components/ui/badge';
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogHeader, 
  DialogTitle 
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { GET_DEPLOYMENT_STATUS, type Deployment, type DeploymentStatus } from '@/lib/graphql';

interface DeploymentDetailsDialogProps {
  deployment: Deployment | null;
  isOpen: boolean;
  onClose: () => void;
  getStatusBadgeColor: (state: string) => string;
}

export function DeploymentDetailsDialog({
  deployment,
  isOpen,
  onClose,
  getStatusBadgeColor
}: DeploymentDetailsDialogProps) {
  const { data: detailStatusData, loading: detailStatusLoading } = useQuery<{
    deploymentStatus: DeploymentStatus;
  }>(GET_DEPLOYMENT_STATUS, {
    variables: { id: deployment?.id },
    skip: !deployment?.id || !isOpen,
    pollInterval: 5000,
    fetchPolicy: 'cache-and-network'
  });

  if (!deployment) return null;

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
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
  );
} 