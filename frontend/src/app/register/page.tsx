'use client';

import { RegisterForm } from '@/components/auth/registerForm';
import { RouteGuard } from '@/components/routeGuard';

export default function RegisterPage() {
  return (
    <RouteGuard requireAuth={false} redirectTo="/">
      <div className="min-h-screen flex items-center justify-center bg-background py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-8">
          <div className="text-center">
            <h2 className="mt-6 text-3xl font-bold text-foreground">
              Create Account
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              Set up your Shipkit deployment environment
            </p>
          </div>
          <RegisterForm />
        </div>
      </div>
    </RouteGuard>
  );
} 