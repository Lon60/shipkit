import { useState } from 'react';
import { z } from 'zod';
import { useForm, Controller, type SubmitHandler } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@apollo/client';
import { toast } from 'sonner';

import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { CodeEditor } from '@/components/ui/code-editor';
import {
  CREATE_DEPLOYMENT,
  GET_DEPLOYMENTS,
  PREVIEW_DEPLOYMENT,
  type Deployment,
} from '@/lib/graphql';

// Schema definitions
const serviceSchema = z.object({
  serviceName: z.string().min(1, 'Service name is required'),
  image: z.string().min(1, 'Image is required'),
  internalPort: z
    .union([z.number().int().positive(), z.string()])
    .optional()
    .transform((v) => (v === '' ? undefined : v))
    .refine(
      (val) => val === undefined || (typeof val === 'number' && val > 0),
      'Port must be a positive number'
    ),
  subDomain: z.string().optional(),
  expose: z.boolean().optional().default(false),
  sslEnabled: z.boolean().optional().default(false),
});

type ServiceForm = z.input<typeof serviceSchema>;

const wizardSchema = z.object({
  name: z
    .string()
    .min(3, 'Deployment name must be at least 3 characters')
    .max(32, 'Name too long'),
  services: z.array(serviceSchema).min(1, 'At least one service is required'),
});

type WizardData = z.input<typeof wizardSchema>;

type Step = 1 | 2 | 3;

interface CreateDeploymentWizardProps {
  onSuccess?: () => void;
}

export function CreateDeploymentWizard({ onSuccess }: CreateDeploymentWizardProps) {
  const [step, setStep] = useState<Step>(1);
  const [previewYaml, setPreviewYaml] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
    watch,
    setValue,
  } = useForm<WizardData, unknown>({
    resolver: zodResolver(wizardSchema),
    defaultValues: { services: [] },
  });

  const services = watch('services') ?? [];

  const [createDeployment] = useMutation<{ createDeployment: Deployment }>(CREATE_DEPLOYMENT, {
    refetchQueries: [{ query: GET_DEPLOYMENTS }],
  });

  const [previewDeployment] = useMutation<{ previewDeployment: string }>(PREVIEW_DEPLOYMENT);

  const nextStep = () => setStep((prev) => (prev < 3 ? ((prev + 1) as Step) : prev));
  const prevStep = () => setStep((prev) => (prev > 1 ? ((prev - 1) as Step) : prev));

  // Service table handlers
  const addService = () => {
    const current = services ?? [];
    const newService: ServiceForm = {
      serviceName: '',
      image: '',
      internalPort: undefined,
      subDomain: undefined,
      expose: false,
      sslEnabled: false,
    };
    setValue('services', [...current, newService]);
  };

  const removeService = (index: number) => {
    if (!services.length) return;
    const copy = [...services];
    copy.splice(index, 1);
    setValue('services', copy);
  };

  const onSubmit: SubmitHandler<WizardData> = async (data) => {
    if (step < 3) {
      nextStep();
      return;
    }

    setIsSubmitting(true);
    try {
      await createDeployment({
        variables: {
          input: {
            name: data.name,
            services: data.services,
            manifestYaml: previewYaml,
          },
        },
      });
      toast.success('Deployment created successfully');
      onSuccess?.();
    } catch (err) {
      toast.error('Failed to create deployment');
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const generatePreview = async () => {
    const data = watch();
    try {
      const res = await previewDeployment({ variables: { input: { ...data, manifestYaml: '' } } });
      setPreviewYaml(res.data?.previewDeployment ?? '');
    } catch (e) {
      toast.error('Preview failed');
      console.error(e);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {step === 1 && (
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Deployment Name</Label>
            <Input id="name" placeholder="my-app" {...register('name')} />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="space-y-4">
          <div className="flex justify-between items-center mb-2">
            <h4 className="font-medium">Services</h4>
            <Button type="button" size="sm" onClick={addService}>
              + Add Service
            </Button>
          </div>
          <div className="space-y-3">
            {services.map((svc, idx) => (
              <div key={idx} className="border p-3 rounded-md space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <Label>Service Name</Label>
                    <Input {...register(`services.${idx}.serviceName` as const)} />
                    {errors.services?.[idx]?.serviceName?.message && (
                      <p className="text-xs text-destructive">
                        {errors.services[idx]?.serviceName?.message}
                      </p>
                    )}
                  </div>
                  <div className="space-y-1">
                    <Label>Image</Label>
                    <Input {...register(`services.${idx}.image` as const)} />
                    {errors.services?.[idx]?.image?.message && (
                      <p className="text-xs text-destructive">
                        {errors.services[idx]?.image?.message}
                      </p>
                    )}
                  </div>
                  <div className="space-y-1">
                    <Label>Internal Port</Label>
                    <Input type="number" {...register(`services.${idx}.internalPort` as const)} />
                    {errors.services?.[idx]?.internalPort?.message && (
                      <p className="text-xs text-destructive">
                        {errors.services[idx]?.internalPort?.message}
                      </p>
                    )}
                  </div>
                  <div className="space-y-1">
                    <Label>Subdomain</Label>
                    <Input placeholder="app" {...register(`services.${idx}.subDomain` as const)} />
                  </div>
                  <div className="flex items-center space-x-2">
                    <Controller
                      name={`services.${idx}.expose` as const}
                      control={control}
                      render={({ field }) => (
                        <Checkbox
                          checked={field.value}
                          onCheckedChange={(checked) => field.onChange(Boolean(checked))}
                        />
                      )}
                    />
                    <Label>Expose</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Controller
                      name={`services.${idx}.sslEnabled` as const}
                      control={control}
                      render={({ field }) => (
                        <Checkbox
                          checked={field.value}
                          onCheckedChange={(checked) => field.onChange(Boolean(checked))}
                        />
                      )}
                    />
                    <Label>SSL Enabled</Label>
                  </div>
                </div>
                <div className="flex justify-end">
                  <Button type="button" variant="destructive" size="sm" onClick={() => removeService(idx)}>
                    Remove
                  </Button>
                </div>
              </div>
            ))}
            {typeof errors.services?.message === 'string' && (
              <p className="text-sm text-destructive">{errors.services.message}</p>
            )}
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="space-y-4">
          <Button type="button" size="sm" onClick={generatePreview}>
            Refresh Preview
          </Button>
          <CodeEditor value={previewYaml} height={300} language="yaml" readOnly />
        </div>
      )}

      <div className="flex justify-between">
        {step > 1 && (
          <Button type="button" variant="outline" onClick={prevStep}>
            Back
          </Button>
        )}
        {step < 3 && (
          <Button type="button" onClick={nextStep}>
            Next
          </Button>
        )}
        {step === 3 && (
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Deploying...' : 'Deploy'}
          </Button>
        )}
      </div>
    </form>
  );
}