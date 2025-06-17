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
  onStart,
  onStop,
  onDelete
}: DeploymentCardProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <Card className="border-border bg-card">
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
          <DeploymentActions
            deployment={deployment}
            isStopped={isStopped}
            status={status}
            stopLoading={stopLoading}
            startLoading={startLoading}
            deleteLoading={deleteLoading}
            onView={onView}
            onStart={onStart}
            onStop={onStop}
            onDelete={onDelete}
          />
        </div>
      </CardHeader>
    </Card>
  );
} 