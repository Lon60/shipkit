'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@apollo/client';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/layout/LoadingSpinner';
import { CHANGE_PASSWORD, type ChangePasswordInput, type AuthPayload } from '@/lib/graphql';
import { KeyRound, CheckCircle, AlertCircle } from 'lucide-react';

const passwordSchema = z.object({
  oldPassword: z.string().min(1, 'Current password is required'),
  newPassword: z.string().min(6, 'New password must be at least 6 characters'),
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "New passwords don't match",
  path: ["confirmPassword"],
});

type PasswordFormData = z.infer<typeof passwordSchema>;

export function PasswordChange() {
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [changePassword] = useMutation<{ changePassword: AuthPayload }>(CHANGE_PASSWORD);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
  });

  const onSubmit = async (data: PasswordFormData) => {
    setIsLoading(true);
    setMessage(null);
    
    try {
      const input: ChangePasswordInput = {
        oldPassword: data.oldPassword,
        newPassword: data.newPassword,
      };

      const { data: result } = await changePassword({
        variables: { input },
      });

      if (result?.changePassword?.token) {
        localStorage.setItem('authToken', result.changePassword.token);
      }

      setMessage({ type: 'success', text: 'Password changed successfully!' });
      reset();
    } catch (error: unknown) {
      console.error('Change password error:', error);
      
      if (error && typeof error === 'object' && 'networkError' in error) {
        const networkError = error.networkError as { statusCode?: number } | null;
        if (networkError?.statusCode === 401) {
          setMessage({ type: 'error', text: 'Current password is incorrect.' });
          return;
        }
      }
      
      if (error && typeof error === 'object' && 'message' in error) {
        const message = error.message as string;
        if (message.includes('Access Denied') || message.includes('Invalid password')) {
          setMessage({ type: 'error', text: 'Current password is incorrect.' });
          return;
        }
      }
      
      toast.error('An unexpected error occurred. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center space-x-2">
          <KeyRound className="h-5 w-5" />
          <CardTitle>Change Password</CardTitle>
        </div>
        <CardDescription>
          Update your account password
        </CardDescription>
      </CardHeader>
      <CardContent>
        {message && (
          <div className={`mb-4 p-3 rounded-md border ${
            message.type === 'success' 
              ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800' 
              : 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'
          }`}>
            <div className="flex items-center space-x-2">
              {message.type === 'success' ? (
                <CheckCircle className="h-4 w-4 text-green-600 dark:text-green-400" />
              ) : (
                <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400" />
              )}
              <p className={`text-sm font-medium ${
                message.type === 'success' 
                  ? 'text-green-700 dark:text-green-300' 
                  : 'text-red-700 dark:text-red-300'
              }`}>
                {message.text}
              </p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="oldPassword">Current Password</Label>
            <Input
              id="oldPassword"
              type="password"
              placeholder="Enter current password"
              {...register('oldPassword')}
            />
            {errors.oldPassword && (
              <p className="text-xs text-destructive">{errors.oldPassword.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="newPassword">New Password</Label>
            <Input
              id="newPassword"
              type="password"
              placeholder="Enter new password"
              {...register('newPassword')}
            />
            {errors.newPassword && (
              <p className="text-xs text-destructive">{errors.newPassword.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Confirm New Password</Label>
            <Input
              id="confirmPassword"
              type="password"
              placeholder="Confirm new password"
              {...register('confirmPassword')}
            />
            {errors.confirmPassword && (
              <p className="text-xs text-destructive">{errors.confirmPassword.message}</p>
            )}
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <Button 
              type="button" 
              variant="outline" 
              onClick={() => {
                reset();
                setMessage(null);
              }}
              disabled={isLoading}
            >
              Clear
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" className="mr-2" />
                  Changing...
                </>
              ) : (
                'Change Password'
              )}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
} 