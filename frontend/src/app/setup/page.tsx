'use client';

import { useState } from 'react';
import { useMutation } from '@apollo/client';
import { SETUP_DOMAIN } from '@/lib/graphql';
import { DashboardLayout } from '@/components/dashboard/layout';
import { PageHeader } from '@/components/layout/PageLayout';
import { Input } from '@/components/ui/input';
import { Button as UiButton } from '@/components/ui/button';
import { toast } from 'sonner';

export default function SetupPage() {
  const [domain, setDomain] = useState('');
  const [setupDomain, { loading }] = useMutation(SETUP_DOMAIN, {
    onCompleted: () => {
      toast.success('Domain configured, reloading...');
      window.location.href = '/';
    },
    onError: (err) => {
      toast.error(err.message, {
        action: {
          label: 'Continue anyway',
          onClick: () => {
            void setupDomain({ variables: { domain, skipValidation: true } });
          },
        },
      });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void setupDomain({ variables: { domain } });
  };

  return (
    <DashboardLayout>
      <div className="max-w-xl mx-auto px-6 py-8">
        <PageHeader title="Platform Setup" description="Configure your custom domain" />
        <form onSubmit={handleSubmit} className="space-y-4 mt-6">
          <Input
            placeholder="example.com"
            value={domain}
            onChange={(e) => setDomain(e.target.value)}
            required
          />
          <UiButton type="submit" disabled={loading}>
            {loading ? 'Saving...' : 'Save Domain'}
          </UiButton>
        </form>
      </div>
    </DashboardLayout>
  );
} 