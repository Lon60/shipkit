'use client';

import { useAuth } from '@/lib/auth';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

interface RouteGuardProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  redirectTo?: string;
}

export function RouteGuard({ 
  children, 
  requireAuth = false, 
  redirectTo 
}: RouteGuardProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return; // Still checking auth state

    if (requireAuth && !isAuthenticated) {
      // Redirect to login if authentication is required but user is not authenticated
      router.push(redirectTo ?? '/login');
      return;
    }

    if (!requireAuth && isAuthenticated && redirectTo) {
      // Redirect authenticated users away from public pages (like login/register)
      router.push(redirectTo);
      return;
    }
  }, [isAuthenticated, isLoading, requireAuth, redirectTo, router]);

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="mt-2 text-sm text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  // Don't render if user should be redirected
  if (requireAuth && !isAuthenticated) {
    return null;
  }

  if (!requireAuth && isAuthenticated && redirectTo) {
    return null;
  }

  return <>{children}</>;
}

// Higher-order component for protected routes
export function withAuth<P extends object>(
  Component: React.ComponentType<P>,
  redirectTo?: string
) {
  return function AuthenticatedComponent(props: P) {
    return (
      <RouteGuard requireAuth={true} redirectTo={redirectTo}>
        <Component {...props} />
      </RouteGuard>
    );
  };
}

// Higher-order component for public routes (redirect if authenticated)
export function withoutAuth<P extends object>(
  Component: React.ComponentType<P>,
  redirectTo?: string
) {
  return function PublicComponent(props: P) {
    return (
      <RouteGuard requireAuth={false} redirectTo={redirectTo}>
        <Component {...props} />
      </RouteGuard>
    );
  };
} 