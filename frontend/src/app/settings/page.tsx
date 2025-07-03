'use client';

import { useState } from 'react';
import { DashboardLayout } from '@/components/dashboard/layout';
import { PageHeader } from '@/components/layout/PageLayout';
import { AccountSettingsForm } from '@/components/dashboard/AccountSettingsForm';
import { AdvancedSettingsForm } from '@/components/dashboard/AdvancedSettingsForm';
import { Button } from '@/components/ui/button';
import { Settings as SettingsIcon, User, Shield } from 'lucide-react';

export default function SettingsPage() {
  const [activeSection, setActiveSection] = useState<'general' | 'advanced'>('general');

  return (
    <DashboardLayout>
      <div className="container max-w-4xl mx-auto p-6">
        <PageHeader 
          title="Settings"
          description="Manage your Shipkit configuration and account preferences"
          icon={<SettingsIcon className="h-6 w-6" />}
        />

        <div className="mt-8">
          <div className="flex space-x-2 mb-8">
            <Button
              variant={activeSection === 'general' ? 'default' : 'outline'}
              onClick={() => setActiveSection('general')}
              className="flex items-center space-x-2"
            >
              <User className="h-4 w-4" />
              <span>General</span>
            </Button>
            <Button
              variant={activeSection === 'advanced' ? 'default' : 'outline'}
              onClick={() => setActiveSection('advanced')}
              className="flex items-center space-x-2"
            >
              <Shield className="h-4 w-4" />
              <span>Advanced</span>
            </Button>
          </div>

          <div>
            {activeSection === 'general' && <AccountSettingsForm />}
            {activeSection === 'advanced' && <AdvancedSettingsForm />}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}