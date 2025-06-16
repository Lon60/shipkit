'use client';

import { DashboardLayout } from '@/components/dashboard/layout';
import { DeploymentsList } from '@/components/dashboard/deploymentsList';

export default function HomePage() {
  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-foreground">Deployments</h1>
          <p className="text-muted-foreground mt-1">
            Manage your Docker container deployments
          </p>
        </div>
        
        <DeploymentsList />
      </div>
    </DashboardLayout>
  );
}
