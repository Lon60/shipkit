import { Button } from '@/components/ui/button';
import { Square, Play, Loader2, Trash2, Edit } from 'lucide-react';

interface DeploymentActionsProps {
  isStopped: boolean;
  status: string;
  stopLoading: boolean;
  startLoading: boolean;
  deleteLoading: boolean;
  onEdit: () => void;
  onStart: () => void;
  onStop: () => void;
  onDelete: () => void;
}

export function DeploymentActions({
  isStopped,
  status,
  stopLoading,
  startLoading,
  deleteLoading,
  onEdit,
  onStart,
  onStop,
  onDelete
}: DeploymentActionsProps) {
  return (
    <div className="flex space-x-2">
      <Button 
        variant="outline" 
        size="sm"
        onClick={onEdit}
      >
        <Edit className="h-4 w-4 mr-2" />
        Edit
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