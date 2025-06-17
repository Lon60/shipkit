'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { LoginForm } from '@/components/auth/loginForm';
import { RouteGuard } from '@/components/routeGuard';
import { useAdminStatus } from '@/lib/hooks/useAdminStatus';
import { AuthLayout } from '@/components/layout/PageLayout';
import { LoadingPage } from '@/components/layout/LoadingSpinner';

export default function LoginPage() {
  const { adminInitialized, loading } = useAdminStatus();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !adminInitialized) {
      router.push('/register');
    }
  }, [adminInitialized, loading, router]);

  if (loading) {
    return <LoadingPage />;
  }

  if (!adminInitialized) {
    return null;
  }

  return (
    <RouteGuard requireAuth={false} redirectTo="/">
      <AuthLayout>
        <div className="w-full max-w-md space-y-8">
          <div className="text-center space-y-4">
            <div className="space-y-2">
              <h1 className="text-4xl font-bold text-primary">
                Shipkit
              </h1>
              <p className="text-lg text-muted-foreground">
                Container Deployment Platform
              </p>
            </div>
            <div className="h-px bg-border"></div>
          </div>
          <LoginForm />
        </div>
      </AuthLayout>
    </RouteGuard>
  );
}