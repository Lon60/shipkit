'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/lib/auth';
import { useRouter, usePathname } from 'next/navigation';
import { Sidebar } from '@/components/dashboard/sidebar';
import { MobileHeader } from '@/components/dashboard/mobileHeader';
import { CreateDeploymentForm } from '@/components/dashboard/createDeploymentForm';
import { LoadingPage } from '@/components/layout/LoadingSpinner';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { usePlatformStatus } from '@/lib/hooks/useAdminStatus';

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);

  const { domainInitialized, loading: statusLoading } = usePlatformStatus();

  useEffect(() => {
    if (!isLoading && !statusLoading && !isAuthenticated) {
      router.push('/login');
    }

    if (!isLoading && !statusLoading && isAuthenticated && !domainInitialized && pathname !== '/setup') {
      router.push('/setup');
    }
  }, [isAuthenticated, isLoading, statusLoading, domainInitialized, pathname, router]);

  const handleCreateDeployment = () => {
    setIsCreateDialogOpen(true);
  };

  const handleDeploymentSuccess = () => {
    setIsCreateDialogOpen(false);
  };

  if (isLoading || statusLoading) {
    return <LoadingPage />;
  }

  if (!isAuthenticated) {
    return null;
  }

  if (isAuthenticated && !domainInitialized && pathname !== '/setup') {
    return null;
  }

  return (
    <div className="min-h-screen bg-background">
      <Sidebar onCreateDeployment={handleCreateDeployment} />
      <MobileHeader onCreateDeployment={handleCreateDeployment} />
      
      <main className="md:ml-64 min-h-screen bg-background px-2 sm:px-0">
        {children}
      </main>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="max-w-2xl w-[95vw] sm:w-full max-h-[85vh] sm:max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Create Deployment</DialogTitle>
            <DialogDescription>
              Deploy your application using Docker Compose configuration
            </DialogDescription>
          </DialogHeader>
          
          <CreateDeploymentForm onSuccess={handleDeploymentSuccess} />
        </DialogContent>
      </Dialog>
    </div>
  );
} 