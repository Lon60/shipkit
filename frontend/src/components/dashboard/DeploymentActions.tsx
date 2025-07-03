import { Button } from '@/components/ui/button';
import { Square, Play, Loader2, Trash2, Edit, MoreVertical } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

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
  const isLoading = stopLoading || startLoading || deleteLoading;

  // Desktop layout (hidden on mobile)
  const DesktopActions = () => (
    <div className="hidden sm:flex space-x-2">
      <Button 
        variant="outline" 
        size="sm"
        onClick={onEdit}
        disabled={isLoading}
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

  // Mobile layout (hidden on desktop)
  const MobileActions = () => (
    <div className="sm:hidden">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
            <MoreVertical className="h-4 w-4" />
            <span className="sr-only">Open menu</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={onEdit} disabled={isLoading}>
            <Edit className="h-4 w-4 mr-2" />
            Edit
          </DropdownMenuItem>
          
          {isStopped ? (
            <DropdownMenuItem
              onClick={onStart}
              disabled={startLoading || status.toLowerCase() === 'starting'}
            >
              {startLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Starting...
                </>
              ) : (
                <>
                  <Play className="h-4 w-4 mr-2" />
                  Start
                </>
              )}
            </DropdownMenuItem>
          ) : (
            <DropdownMenuItem
              onClick={onStop}
              disabled={stopLoading || status.toLowerCase() === 'stopping'}
            >
              {stopLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Stopping...
                </>
              ) : (
                <>
                  <Square className="h-4 w-4 mr-2" />
                  Stop
                </>
              )}
            </DropdownMenuItem>
          )}
          
          <DropdownMenuItem
            onClick={onDelete}
            disabled={deleteLoading}
            variant="destructive"
          >
            {deleteLoading ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Deleting...
              </>
            ) : (
              <>
                <Trash2 className="h-4 w-4 mr-2" />
                Delete
              </>
            )}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );

  return (
    <>
      <DesktopActions />
      <MobileActions />
    </>
  );
} 