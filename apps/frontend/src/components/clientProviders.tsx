'use client';

import { ApolloProvider } from '@apollo/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { Toaster } from '@/components/ui/sonner';
import { apolloClient } from '@/lib/apollo';
import { AuthProvider } from '@/lib/auth';
import { trpc, trpcClient } from '@/utils/trpc';

export function ClientProviders({ children }: Readonly<{ children: React.ReactNode }>) {
  const [queryClient] = useState(() => new QueryClient());

  return (
    <trpc.Provider client={trpcClient} queryClient={queryClient}>
      <QueryClientProvider client={queryClient}>
        <ApolloProvider client={apolloClient}>
          <AuthProvider>
            {children}
            <Toaster />
          </AuthProvider>
        </ApolloProvider>
      </QueryClientProvider>
    </trpc.Provider>
  );
} 