import { Button } from '@/components/ui/button';
import { Square, Eye, Play, Loader2, Trash2 } from 'lucide-react';
import { type Deployment } from '@/lib/graphql';

interface DeploymentActionsProps {
  deployment: Deployment;
  isStopped: boolean;
  status: string;
  stopLoading: boolean;
  startLoading: boolean;
  deleteLoading: boolean;
  onView: () => void;
  onStart: () => void;
  onStop: () => void;
  onDelete: () => void;
}

export function DeploymentActions({
  deployment,
  isStopped,
  status,
  stopLoading,
  startLoading,
  deleteLoading,
  onView,
  onStart,
  onStop,
  onDelete
}: DeploymentActionsProps) {
  return (
    <div className="flex space-x-2">
      <Button 
        variant="outline" 
        size="sm"
        onClick={onView}
      >
        <Eye className="h-4 w-4 mr-2" />
        View
      </Button>
      
      {isStopped ? (
        <Button 
          variant="default" 
          size="sm"
          disabled={startLoading || status.toLowerCase() === 'starting'}
          onClick={onStart}
        >
          {startLoading ? (
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
          disabled={stopLoading || status.toLowerCase() === 'stopping'}
          onClick={onStop}
        >
          {stopLoading ? (
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
        disabled={deleteLoading}
        onClick={onDelete}
      >
        {deleteLoading ? (
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
  );
} 