'use client';

import { DashboardLayout } from '@/components/dashboard/layout';
import { DeploymentsList } from '@/components/dashboard/deploymentsList';
import { PageHeader } from '@/components/layout/PageLayout';

export default function HomePage() {
  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto px-6 py-8">
        <PageHeader 
          title="Deployments"
          description="Manage your Docker container deployments"
        />
        <DeploymentsList />
      </div>
    </DashboardLayout>
  );
}
