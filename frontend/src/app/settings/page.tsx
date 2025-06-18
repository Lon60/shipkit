'use client';

import { DashboardLayout } from '@/components/dashboard/layout';
import { PageHeader } from '@/components/layout/PageLayout';
import { ChangePasswordForm } from '@/components/dashboard/changePasswordForm';
import { Settings as SettingsIcon } from 'lucide-react';

export default function SettingsPage() {
  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto px-6 py-8">
        <PageHeader 
          title="Settings"
          description="Manage your Shipkit preferences and configuration"
          icon={<SettingsIcon className="h-6 w-6" />}
        />

        <div className="space-y-6">
          <ChangePasswordForm />
        </div>
      </div>
    </DashboardLayout>
  );
} 