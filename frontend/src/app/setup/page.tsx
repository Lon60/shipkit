'use client';

import { useState } from 'react';
import { useMutation } from '@apollo/client';
import { SETUP_DOMAIN } from '@/lib/graphql';
import { DashboardLayout } from '@/components/dashboard/layout';
import { PageHeader } from '@/components/layout/PageLayout';
import { Input } from '@/components/ui/input';
import { Button as UiButton } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';

export default function SetupPage() {
  const [domain, setDomain] = useState('');
  const [sslEnabled, setSslEnabled] = useState(true);
  const [forceSsl, setForceSsl] = useState(true);

  const [setupDomain, { loading }] = useMutation(SETUP_DOMAIN, {
    onCompleted: (_data, variables) => {
      const { domain: fqdn, sslEnabled } = (variables as any) ?? { domain: '', sslEnabled: true };
      const protocol = sslEnabled ? 'https://' : 'http://';
      toast.success('Domain configured, redirecting...');
      window.location.href = `${protocol}${fqdn}`;
    },
    onError: (err) => {
      toast.error(err.message, {
        action: {
          label: 'Continue anyway',
          onClick: () => {
            void setupDomain({ variables: { domain, skipValidation: true, sslEnabled, forceSsl } });
          },
        },
      });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void setupDomain({ variables: { domain, sslEnabled, forceSsl } });
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
          <div className="flex items-center space-x-2">
            <Checkbox
              id="sslEnabled"
              checked={sslEnabled}
              onCheckedChange={(checked) => {
                const isChecked = Boolean(checked);
                setSslEnabled(isChecked);
                if (!isChecked) {
                  setForceSsl(false);
                }
              }}
            />
            <Label htmlFor="sslEnabled">Enable SSL (HTTPS)</Label>
          </div>
          <div className="flex items-center space-x-2">
            <Checkbox
              id="forceSsl"
              checked={forceSsl}
              onCheckedChange={(checked) => setForceSsl(Boolean(checked))}
              disabled={!sslEnabled}
            />
            <Label htmlFor="forceSsl">Force HTTPS (Redirect all HTTP traffic to HTTPS)</Label>
          </div>
          <UiButton type="submit" disabled={loading}>
            {loading ? 'Saving...' : 'Save Domain'}
          </UiButton>
        </form>
      </div>
    </DashboardLayout>
  );
} 