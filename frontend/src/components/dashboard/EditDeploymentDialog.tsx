'use client';

import { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@apollo/client';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { CodeEditor } from '@/components/ui/code-editor';
import { UPDATE_DEPLOYMENT, GET_DEPLOYMENTS, type Deployment } from '@/lib/graphql';

const updateDeploymentSchema = z.object({
  name: z.string().min(1, 'Deployment name is required').min(3, 'Name must be at least 3 characters'),
  composeYaml: z.string().min(1, 'Docker Compose YAML is required').refine(
    (val) => {
      try {
        const lowerVal = val.toLowerCase();
        return lowerVal.includes('services');
      } catch {
        return false;
      }
    },
    { message: 'Invalid Docker Compose YAML format' }
  ),
});

type UpdateDeploymentFormData = z.infer<typeof updateDeploymentSchema>;

interface EditDeploymentDialogProps {
  deployment: Deployment | null;
  isOpen: boolean;
  onClose: () => void;
}

export function EditDeploymentDialog({ deployment, isOpen, onClose }: EditDeploymentDialogProps) {
  const [isLoading, setIsLoading] = useState(false);

  const [updateDeployment] = useMutation<{ updateDeployment: Deployment }>(UPDATE_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<UpdateDeploymentFormData>({
    resolver: zodResolver(updateDeploymentSchema),
  });

  // Reset form when deployment changes
  useEffect(() => {
    if (deployment) {
      reset({
        name: deployment.name,
        composeYaml: deployment.composeYaml,
      });
    }
  }, [deployment, reset]);

  const onSubmit = async (data: UpdateDeploymentFormData) => {
    if (!deployment) return;
    
    setIsLoading(true);
    try {
      await updateDeployment({
        variables: { 
          id: deployment.id,
          input: {
            name: data.name,
            composeYaml: data.composeYaml
          }
        },
      });
      toast.success('Deployment updated successfully');
      onClose();
    } catch (error) {
      console.error('Update deployment error:', error);
      toast.error('Failed to update deployment. Check your configuration.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Edit Deployment</DialogTitle>
        </DialogHeader>
        
        {deployment && (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name" className="text-foreground">Deployment Name</Label>
              <Input
                id="name"
                placeholder="my-awesome-app"
                className="bg-background border-border text-foreground"
                {...register('name')}
              />
              {errors.name && (
                <p className="text-sm text-destructive">{errors.name.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="composeYaml" className="text-foreground">Docker Compose YAML</Label>
              <Controller
                name="composeYaml"
                control={control}
                render={({ field }) => (
                  <CodeEditor
                    value={field.value}
                    onChange={(value) => field.onChange(value ?? '')}
                    height={300}
                    language="yaml"
                    className={errors.composeYaml ? 'border-destructive' : ''}
                  />
                )}
              />
              {errors.composeYaml && (
                <p className="text-sm text-destructive">{errors.composeYaml.message}</p>
              )}
            </div>

            <div className="flex justify-end space-x-2">
              <Button type="button" variant="outline" onClick={handleClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? 'Updating...' : 'Update Deployment'}
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
} 