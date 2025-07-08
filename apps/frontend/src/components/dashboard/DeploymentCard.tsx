import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { DeploymentActions } from './DeploymentActions';
import { type Deployment } from '@/lib/graphql';

interface DeploymentCardProps {
  deployment: Deployment;
  status: string;
  isStopped: boolean;
  getStatusBadgeColor: (state: string) => string;
  stopLoading: boolean;
  startLoading: boolean;
  deleteLoading: boolean;
  onView: () => void;
  onEdit: () => void;
  onStart: () => void;
  onStop: () => void;
  onDelete: () => void;
}

export function DeploymentCard({
  deployment,
  status,
  isStopped,
  getStatusBadgeColor,
  stopLoading,
  startLoading,
  deleteLoading,
  onView,
  onEdit,
  onStart,
  onStop,
  onDelete
}: DeploymentCardProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const handleCardClick = () => {
    onView();
  };

  const handleActionsClick = (e: React.MouseEvent) => {
    e.stopPropagation();
  };

  return (
    <Card 
      className="border-border bg-card cursor-pointer hover:bg-accent/50 transition-colors" 
      onClick={handleCardClick}
    >
      <CardHeader className="pb-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-start">
          <div className="flex-1 min-w-0">
            <CardTitle className="text-base text-foreground mb-2">
              {deployment.name}
            </CardTitle>
            <CardDescription className="text-muted-foreground space-y-1">
              <div className="text-xs">
                ID: {deployment.id}
              </div>
              <div className="text-xs">
                Created: {formatDate(deployment.createdAt)}
              </div>
              {status !== 'unknown' && (
                <div className="pt-1">
                  <Badge className={getStatusBadgeColor(status)} variant="outline">
                    {status}
                  </Badge>
                </div>
              )}
            </CardDescription>
          </div>
          <div className="flex-shrink-0 self-start" onClick={handleActionsClick}>
            <DeploymentActions
              isStopped={isStopped}
              status={status}
              stopLoading={stopLoading}
              startLoading={startLoading}
              deleteLoading={deleteLoading}
              onEdit={onEdit}
              onStart={onStart}
              onStop={onStop}
              onDelete={onDelete}
            />
          </div>
        </div>
      </CardHeader>
    </Card>
  );
} 