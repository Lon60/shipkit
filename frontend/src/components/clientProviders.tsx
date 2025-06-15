'use client';

import { ApolloProvider } from '@apollo/client';
import { Toaster } from '@/components/ui/sonner';
import { apolloClient } from '@/lib/apollo';
import { AuthProvider } from '@/lib/auth';

export function ClientProviders({ children }: { children: React.ReactNode }) {
  return (
    <ApolloProvider client={apolloClient}>
      <AuthProvider>
        {children}
        <Toaster />
      </AuthProvider>
    </ApolloProvider>
  );
} 