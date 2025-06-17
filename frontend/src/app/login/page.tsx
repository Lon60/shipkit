'use client';

import { LoginForm } from '@/components/auth/loginForm';
import { RouteGuard } from '@/components/routeGuard';

export default function LoginPage() {
  return (
    <RouteGuard requireAuth={false} redirectTo="/">
      <div className="min-h-screen flex items-center justify-center bg-background py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-8">
          <div className="text-center">
            <h2 className="mt-6 text-3xl font-bold text-foreground">
              Shipkit
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              Sign in to manage your deployments
            </p>
          </div>
          <LoginForm />
        </div>
      </div>
    </RouteGuard>
  );
}