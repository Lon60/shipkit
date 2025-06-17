'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/lib/auth';
import { useRouter } from 'next/navigation';
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

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, isLoading, router]);

  const handleCreateDeployment = () => {
    setIsCreateDialogOpen(true);
  };

  const handleDeploymentSuccess = () => {
    setIsCreateDialogOpen(false);
  };

  if (isLoading) {
    return <LoadingPage />;
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-background">
      <Sidebar onCreateDeployment={handleCreateDeployment} />
      <MobileHeader onCreateDeployment={handleCreateDeployment} />
      
      <main className="md:ml-64 min-h-screen bg-background">
        {children}
      </main>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
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