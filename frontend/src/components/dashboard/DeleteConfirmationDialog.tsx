import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogHeader, 
  DialogTitle 
} from '@/components/ui/dialog';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { type Deployment } from '@/lib/graphql';

interface DeleteConfirmationDialogProps {
  isOpen: boolean;
  deployment: Deployment | null;
  deleteLoading: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function DeleteConfirmationDialog({
  isOpen,
  deployment,
  deleteLoading,
  onClose,
  onConfirm
}: DeleteConfirmationDialogProps) {
  const [confirmText, setConfirmText] = useState('');

  const handleClose = () => {
    setConfirmText('');
    onClose();
  };

  const isConfirmDisabled = !deployment || confirmText !== deployment.name || deleteLoading;

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
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
        
        {deployment && (
          <div className="space-y-4">
            <div>
              <p className="text-sm font-medium mb-2">
                Please type <span className="font-mono bg-muted px-1 rounded text-destructive">{deployment.name}</span> to confirm:
              </p>
              <Input
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
                placeholder="Enter deployment name"
                className="font-mono"
              />
            </div>
            
            <div className="flex justify-end space-x-2">
              <Button variant="outline" onClick={handleClose}>
                Cancel
              </Button>
              <Button 
                variant="destructive" 
                disabled={isConfirmDisabled}
                onClick={onConfirm}
              >
                {deleteLoading ? (
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
  );
} 