'use client';

import { useState } from 'react';
import { useAuth } from '@/lib/auth';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { Header } from '@/components/dashboard/header';
import { DeploymentsList } from '@/components/dashboard/deploymentsList';
import { CreateDeploymentForm } from '@/components/dashboard/createDeploymentForm';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

export default function HomePage() {
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
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null; // Will redirect to login
  }

  // Dashboard content for authenticated users
  return (
    <div className="min-h-screen bg-background">
      <Header onCreateDeployment={handleCreateDeployment} />
      
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <DeploymentsList />
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
