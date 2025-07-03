import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogHeader, 
  DialogTitle 
} from '@/components/ui/dialog';
import { Loader2, Trash2 } from 'lucide-react';
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
  const handleClose = () => {
    if (!deleteLoading) {
      onClose();
    }
  };

  const handleConfirm = () => {
    onConfirm();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-md w-[95vw] sm:w-full">
        <DialogHeader>
          <DialogTitle>Delete Deployment</DialogTitle>
          <DialogDescription>
            Are you sure you want to delete this deployment?
          </DialogDescription>
        </DialogHeader>
        
        <div className="space-y-4">
          <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
            <p className="text-sm font-medium text-destructive">
              Warning: This action cannot be undone
            </p>
            <p className="text-sm text-muted-foreground mt-1">
              Deployment &quot;{deployment?.name}&quot; will be permanently deleted and all associated containers will be stopped.
            </p>
          </div>
          
          <div className="flex flex-col sm:flex-row gap-3 pt-4">
            <Button 
              variant="outline" 
              onClick={handleClose}
              className="flex-1"
              disabled={deleteLoading}
            >
              Cancel
            </Button>
            <Button 
              variant="destructive" 
              onClick={onConfirm}
              disabled={deleteLoading}
              className="flex-1"
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
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
} 