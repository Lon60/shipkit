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
import { useAuth } from '@/lib/auth';
import { KeyRound, CheckCircle, AlertCircle } from 'lucide-react';

const changePasswordSchema = z.object({
  oldPassword: z.string().min(1, 'Current password is required'),
  newPassword: z.string().min(6, 'New password must be at least 6 characters'),
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "New passwords don't match",
  path: ["confirmPassword"],
});

type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

export function ChangePasswordForm() {
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const { token: currentToken } = useAuth();

  const [changePassword] = useMutation<{ changePassword: AuthPayload }>(CHANGE_PASSWORD);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
  });

  const onSubmit = async (data: ChangePasswordFormData) => {
    setIsLoading(true);
    setSuccessMessage(null);
    setErrorMessage(null);
    
    try {
      const input: ChangePasswordInput = {
        oldPassword: data.oldPassword,
        newPassword: data.newPassword,
      };

      const { data: result } = await changePassword({
        variables: { input },
      });

      // Update the stored token with the new one returned from the mutation
      if (result?.changePassword?.token) {
        localStorage.setItem('authToken', result.changePassword.token);
      }

      setSuccessMessage('Password changed successfully!');
      reset();
    } catch (error: any) {
      console.error('Change password error:', error);
      
      // Check if it's a 401 error or authentication related error
      if (error?.networkError?.statusCode === 401 || 
          error?.message?.includes('Access Denied') || 
          error?.message?.includes('Unauthorized') ||
          error?.message?.includes('Invalid password') ||
          error?.graphQLErrors?.some((e: any) => e.message?.includes('password'))) {
        setErrorMessage('Current password is incorrect. Please try again.');
      } else {
        // For other errors, still use toast as they might be network/server issues
        toast.error('An unexpected error occurred. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="bg-card border-border">
      <CardHeader>
        <div className="flex items-center space-x-2">
          <KeyRound className="h-5 w-5 text-card-foreground" />
          <CardTitle className="text-card-foreground">Change Password</CardTitle>
        </div>
        <CardDescription className="text-muted-foreground">
          Update your account password to keep your account secure
        </CardDescription>
      </CardHeader>
      <CardContent>
        {/* Success Message */}
        {successMessage && (
          <div className="mb-4 p-3 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-md">
            <div className="flex items-center space-x-2">
              <CheckCircle className="h-4 w-4 text-green-600 dark:text-green-400" />
              <p className="text-sm text-green-700 dark:text-green-300">{successMessage}</p>
            </div>
          </div>
        )}

        {/* Error Message */}
        {errorMessage && (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
            <div className="flex items-center space-x-2">
              <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400" />
              <p className="text-sm text-red-700 dark:text-red-300">{errorMessage}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="oldPassword" className="text-sm font-medium text-card-foreground">
              Current Password
            </Label>
            <Input
              id="oldPassword"
              type="password"
              placeholder="Enter your current password"
              className="h-11"
              {...register('oldPassword')}
            />
            {errors.oldPassword && (
              <p className="text-xs text-destructive mt-1">
                {errors.oldPassword.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="newPassword" className="text-sm font-medium text-card-foreground">
              New Password
            </Label>
            <Input
              id="newPassword"
              type="password"
              placeholder="Enter your new password"
              className="h-11"
              {...register('newPassword')}
            />
            {errors.newPassword && (
              <p className="text-xs text-destructive mt-1">
                {errors.newPassword.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirmPassword" className="text-sm font-medium text-card-foreground">
              Confirm New Password
            </Label>
            <Input
              id="confirmPassword"
              type="password"
              placeholder="Confirm your new password"
              className="h-11"
              {...register('confirmPassword')}
            />
            {errors.confirmPassword && (
              <p className="text-xs text-destructive mt-1">
                {errors.confirmPassword.message}
              </p>
            )}
          </div>

          <div className="flex justify-end space-x-2 pt-4">
            <Button type="button" variant="outline" onClick={() => {
              reset();
              setSuccessMessage(null);
              setErrorMessage(null);
            }}>
              Clear
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? (
                <div className="flex items-center space-x-2">
                  <LoadingSpinner size="sm" />
                  <span>Updating...</span>
                </div>
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