'use client';

import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@apollo/client';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { CodeEditor } from '@/components/ui/code-editor';
import { CREATE_DEPLOYMENT, GET_DEPLOYMENTS, type Deployment } from '@/lib/graphql';

const deploymentSchema = z.object({
  name: z.string().min(1, 'Deployment name is required').min(3, 'Name must be at least 3 characters'),
  manifestYaml: z.string().min(1, 'Kubernetes Manifest YAML is required'),
});

type DeploymentFormData = z.infer<typeof deploymentSchema>;

interface CreateDeploymentFormProps {
  onSuccess?: () => void;
}

export function CreateDeploymentForm({ onSuccess }: CreateDeploymentFormProps) {
  const [isLoading, setIsLoading] = useState(false);

  const [createDeployment] = useMutation<{ createDeployment: Deployment }>(CREATE_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<DeploymentFormData>({
    resolver: zodResolver(deploymentSchema),
  });

  const onSubmit = async (data: DeploymentFormData) => {
    setIsLoading(true);
    try {
      await createDeployment({
        variables: { 
          input: {
            name: data.name,
            manifestYaml: data.manifestYaml
          }
        },
      });
      toast.success('Deployment created successfully');
      reset();
      onSuccess?.();
    } catch (error) {
      console.error('Deployment error:', error);
      toast.error('Failed to create deployment. Check your configuration.');
    } finally {
      setIsLoading(false);
    }
  };



  return (
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
        <Label htmlFor="manifestYaml" className="text-foreground">Kubernetes Manifest YAML</Label>
        <Controller
          name="manifestYaml"
          control={control}
          render={({ field }) => (
            <CodeEditor
              value={field.value}
              onChange={(value) => field.onChange(value ?? '')}
              height={300}
              language="yaml"
              className={errors.manifestYaml ? 'border-destructive' : ''}
            />
          )}
        />
        {errors.manifestYaml && (
          <p className="text-sm text-destructive">{errors.manifestYaml.message}</p>
        )}
      </div>

      <div className="flex justify-end space-x-2">
        <Button type="button" variant="outline" onClick={() => reset()}>
          Clear
        </Button>
        <Button type="submit" disabled={isLoading}>
          {isLoading ? 'Creating...' : 'Create Deployment'}
        </Button>
      </div>
    </form>
  );
} 