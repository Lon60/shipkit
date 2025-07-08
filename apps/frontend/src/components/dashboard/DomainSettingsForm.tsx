'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@apollo/client';
import { SETUP_DOMAIN, PLATFORM_SETTINGS, type PlatformSetting } from '@/lib/graphql';
import { parseDomainError, type DomainError } from '@/lib/utils';
import { ErrorCode } from '@/lib/graphql';
import { Input } from '@/components/ui/input';
import { Button as UiButton } from '@/components/ui/button';
import { DomainValidationNotice } from '@/components/ui/domain-validation-notice';
import { CertificateErrorNotice } from '@/components/ui/certificate-error-notice';
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
  const [domainError, setDomainError] = useState<DomainError | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const { data, loading: queryLoading, error: queryError } = useQuery<{ platformSettings: PlatformSetting }>(PLATFORM_SETTINGS);

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
            <>
              {domainError.code === ErrorCode.DOMAIN_VALIDATION_ERROR && (
                <DomainValidationNotice
                  onContinueAnyway={handleContinueAnyway}
                  onTryAgain={() => setDomainError(null)}
                  isLoading={mutationLoading || isProcessing}
                />
              )}
              {domainError.code === ErrorCode.CERTIFICATE_ISSUANCE_ERROR && (
                <CertificateErrorNotice
                  onTryAgain={() => setDomainError(null)}
                  isLoading={mutationLoading || isProcessing}
                />
              )}
            </>
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
            <Label htmlFor="forceSsl">Force HTTPS (Redirect all HTTP traffic to HTTPS)</Label>
          </div>

          {isProcessing && sslEnabled && (
            <div className="p-4 bg-blue-50 dark:bg-blue-950/50 border border-blue-200 dark:border-blue-800 rounded-md">
              <div className="flex gap-3">
                <Clock className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0 animate-spin" />
                <div>
                  <p className="text-sm font-medium text-blue-800 dark:text-blue-200">Processing SSL Certificate</p>
                  <p className="text-sm text-blue-600 dark:text-blue-300">
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