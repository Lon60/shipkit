'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { RegisterForm } from '@/components/auth/registerForm';
import { RouteGuard } from '@/components/routeGuard';
import { useAdminStatus } from '@/lib/hooks/useAdminStatus';
import { SplitLayout } from '@/components/layout/PageLayout';
import { LoadingPage } from '@/components/layout/LoadingSpinner';

export default function RegisterPage() {
  const { adminInitialized, loading } = useAdminStatus();
  const router = useRouter();

  useEffect(() => {
    if (!loading && adminInitialized) {
      router.push('/login');
    }
  }, [adminInitialized, loading, router]);

  if (loading) {
    return <LoadingPage />;
  }

  if (adminInitialized) {
    return null;
  }
  
  const leftContent = (
    <div className="max-w-lg space-y-8">
      {/* Main Header */}
      <div className="space-y-4">
        <h1 className="text-4xl lg:text-5xl font-bold text-foreground">
          Welcome to
          <span className="block text-primary mt-2">
            Shipkit
          </span>
        </h1>
        <p className="text-lg text-muted-foreground">
          Your new container deployment platform instance
        </p>
      </div>

      {/* Setup Instructions */}
      <div className="space-y-6">
        <div className="space-y-3">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center">
              <svg className="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-foreground">
              Setup Your Admin Account
            </h3>
          </div>
          <p className="text-muted-foreground ml-11">
            This is a fresh Shipkit instance. Create the first admin account to get started with container deployments.
          </p>
        </div>

        {/* Security Warning */}
        <div className="border-l-4 border-amber-400 bg-amber-50 dark:bg-amber-900/20 dark:border-amber-500 p-4 rounded-r-lg">
          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-amber-600 dark:text-amber-400 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L5.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            <div>
              <h4 className="font-medium text-amber-800 dark:text-amber-200">
                Security Notice
              </h4>
              <p className="text-sm text-amber-700 dark:text-amber-300 mt-1">
                Please register immediately to secure your instance. This registration endpoint will be disabled once an admin account is created.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const rightContent = (
    <div className="w-full max-w-md">
      <RegisterForm />
    </div>
  );

  return (
    <RouteGuard requireAuth={false} redirectTo="/">
      <SplitLayout 
        leftContent={leftContent}
        rightContent={rightContent}
      />
    </RouteGuard>
  );
}