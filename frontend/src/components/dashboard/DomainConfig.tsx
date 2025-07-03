'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@apollo/client';
import { SETUP_DOMAIN, PLATFORM_SETTINGS, type PlatformSetting } from '@/lib/graphql';
import { parseDomainError, type DomainError } from '@/lib/utils';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { DomainErrorDisplay } from '@/components/ui/domainErrorDisplay';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { Clock, Globe } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/layout/LoadingSpinner';

export function DomainConfig() {
  const [domain, setDomain] = useState('');
  const [sslEnabled, setSslEnabled] = useState(true);
  const [forceSsl, setForceSsl] = useState(true);
  const [domainError, setDomainError] = useState<DomainError | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const { data, loading } = useQuery<{ platformSettings: PlatformSetting }>(PLATFORM_SETTINGS);

  useEffect(() => {
    if (data?.platformSettings) {
      setDomain(data.platformSettings.fqdn);
      setSslEnabled(data.platformSettings.sslEnabled);
      setForceSsl(data.platformSettings.forceSsl);
    }
  }, [data]);

  const [setupDomain, { loading: mutationLoading }] = useMutation(SETUP_DOMAIN, {
    onCompleted: () => {
      toast.success('Domain configured successfully!');
      setIsProcessing(false);
    },
    onError: (err) => {
      setIsProcessing(false);
      const parsedError = parseDomainError(err);
      if (parsedError) {
        setDomainError(parsedError);
      } else {
        toast.error(err.message);
      }
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setDomainError(null);
    setIsProcessing(true);
    void setupDomain({ variables: { domain, sslEnabled, forceSsl } });
  };

  const handleContinueAnyway = () => {
    setDomainError(null);
    setIsProcessing(true);
    void setupDomain({ variables: { domain, skipValidation: true, sslEnabled, forceSsl } });
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Globe className="h-5 w-5" />
            <CardTitle>Domain Configuration</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="flex justify-center py-8">
          <LoadingSpinner size="lg" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center space-x-2">
          <Globe className="h-5 w-5" />
          <CardTitle>Domain Configuration</CardTitle>
        </div>
        <CardDescription>
          Configure your custom domain and SSL settings
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="domain">Custom Domain</Label>
            <Input
              id="domain"
              placeholder="example.com"
              value={domain}
              onChange={(e) => setDomain(e.target.value)}
              required
            />
          </div>

          {domainError && (
            <DomainErrorDisplay
              error={domainError}
              onContinueAnyway={handleContinueAnyway}
              onTryAgain={() => setDomainError(null)}
              isLoading={mutationLoading || isProcessing}
            />
          )}

          <div className="space-y-3">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="sslEnabled"
                checked={sslEnabled}
                onCheckedChange={(checked) => {
                  const isChecked = Boolean(checked);
                  setSslEnabled(isChecked);
                  if (!isChecked) setForceSsl(false);
                }}
              />
              <Label htmlFor="sslEnabled" className="text-sm">Enable SSL (HTTPS)</Label>
            </div>

            <div className="flex items-center space-x-2">
              <Checkbox
                id="forceSsl"
                checked={forceSsl}
                onCheckedChange={(checked) => setForceSsl(Boolean(checked))}
                disabled={!sslEnabled}
              />
              <Label htmlFor="forceSsl" className="text-sm">Force HTTPS redirect</Label>
            </div>
          </div>

          {isProcessing && sslEnabled && (
            <div className="p-3 bg-blue-50 dark:bg-blue-950/50 border border-blue-200 dark:border-blue-800 rounded-md">
              <div className="flex gap-2">
                <Clock className="h-4 w-4 text-blue-600 dark:text-blue-400 mt-0.5 animate-spin" />
                <div className="text-sm">
                  <p className="font-medium text-blue-800 dark:text-blue-200">Processing SSL Certificate</p>
                  <p className="text-blue-600 dark:text-blue-300 text-xs">This may take a few minutes...</p>
                </div>
              </div>
            </div>
          )}

          <div className="flex justify-end">
            <Button type="submit" disabled={mutationLoading || isProcessing}>
              {isProcessing ? (
                <>
                  <LoadingSpinner size="sm" className="mr-2" />
                  Processing...
                </>
              ) : (
                'Save Domain'
              )}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
} 