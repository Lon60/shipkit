'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@apollo/client';
import { SETUP_DOMAIN, PLATFORM_SETTINGS, type PlatformSetting } from '@/lib/graphql';
import { Input } from '@/components/ui/input';
import { Button as UiButton } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/layout/LoadingSpinner';

export function DomainSettingsForm() {
  const [domain, setDomain] = useState('');
  const [sslEnabled, setSslEnabled] = useState(true);
  const [forceSsl, setForceSsl] = useState(true);
  const [domainError, setDomainError] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const { data, loading: queryLoading, error: queryError } = useQuery<{ platformSettings: PlatformSetting }>(PLATFORM_SETTINGS);

  useEffect(() => {
    if (data?.platformSettings) {
      setDomain(data.platformSettings.fqdn);
      // Assuming sslEnabled and forceSsl are part of platformSettings, 
      // but they are not in the current schema. 
      // For now, we'll keep their default values or add them to the schema if needed.
      // setSslEnabled(data.platformSettings.sslEnabled);
      // setForceSsl(data.platformSettings.forceSsl);
    }
  }, [data]);

  const [setupDomain, { loading: mutationLoading }] = useMutation(SETUP_DOMAIN, {
    onCompleted: () => {
      toast.success('Domain configured successfully!');
      setIsProcessing(false);
    },
    onError: (err) => {
      setIsProcessing(false);
      if (err.message.includes('does not resolve') || err.message.includes('Configure an A record')) {
        const serverIpRegex = /pointing to ([\d.]+)/;
        const serverIpMatch = serverIpRegex.exec(err.message);
        const serverIp = serverIpMatch ? serverIpMatch[1] : 'your server IP';
        setDomainError(`Domain resolution failed. Please create a DNS A record for '${domain}' pointing to ${serverIp}`);
      } else {
        setDomainError(err.message);
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

  if (queryLoading) {
    return (
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle>Domain Configuration</CardTitle>
          <CardDescription>Loading current domain settings...</CardDescription>
        </CardHeader>
        <CardContent className="flex justify-center items-center h-32">
          <LoadingSpinner size="lg" />
        </CardContent>
      </Card>
    );
  }

  if (queryError) {
    return (
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle>Domain Configuration</CardTitle>
          <CardDescription>Error loading domain settings.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-md">
            <div className="flex gap-3">
              <AlertCircle className="h-5 w-5 text-destructive mt-0.5 flex-shrink-0" />
              <p className="text-sm text-destructive">Error: {queryError.message}</p>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="bg-card border-border">
      <CardHeader>
        <CardTitle>Domain Configuration</CardTitle>
        <CardDescription>Update your custom domain and SSL settings.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="domain">Domain</Label>
            <Input
              id="domain"
              placeholder="example.com"
              value={domain}
              onChange={(e) => setDomain(e.target.value)}
              required
            />
          </div>

          {domainError && (
            <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-md">
              <div className="flex gap-3">
                <AlertCircle className="h-5 w-5 text-destructive mt-0.5 flex-shrink-0" />
                <div className="space-y-2">
                  <p className="text-sm font-medium text-destructive">Domain Setup Required</p>
                  <p className="text-sm text-muted-foreground">{domainError}</p>
                  <div className="flex gap-2 mt-3">
                    <UiButton
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleContinueAnyway}
                      disabled={mutationLoading || isProcessing}
                    >
                      Continue Anyway
                    </UiButton>
                    <UiButton
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setDomainError(null)}
                    >
                      Try Again
                    </UiButton>
                  </div>
                </div>
              </div>
            </div>
          )}

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
            <Label htmlFor="forceSsl">Force HTTPS</Label>
          </div>

          {isProcessing && sslEnabled && (
            <div className="p-4 bg-blue-50 border border-blue-200 rounded-md">
              <div className="flex gap-3">
                <Clock className="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-sm font-medium text-blue-800">Processing SSL Certificate</p>
                  <p className="text-sm text-blue-600">
                    Requesting SSL certificate from Let&apos;s Encrypt. This may take a few minutes...
                  </p>
                </div>
              </div>
            </div>
          )}

          <UiButton type="submit" disabled={mutationLoading || isProcessing} className="w-full">
            {isProcessing ? (
              <>
                <Clock className="h-4 w-4 mr-2 animate-spin" />
                {sslEnabled ? 'Processing SSL Certificate...' : 'Configuring Domain...'}
              </>
            ) : mutationLoading ? (
              <>
                <Clock className="h-4 w-4 mr-2 animate-spin" />
                Validating...
              </>
            ) : (
              <>
                <CheckCircle className="h-4 w-4 mr-2" />
                Save Changes
              </>
            )}
          </UiButton>
        </form>
      </CardContent>
    </Card>
  );
}