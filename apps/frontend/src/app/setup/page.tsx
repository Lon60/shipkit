'use client';

import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@apollo/client';
import { SETUP_DOMAIN, GET_STATUS, PLATFORM_SETTINGS, type PlatformStatus, type PlatformSetting } from '@/lib/graphql';
import { parseDomainError, type DomainError } from '@/lib/utils';
import { ErrorCode } from '@/lib/graphql';
import { useAuth } from '@/lib/auth';
import { PageHeader } from '@/components/layout/PageLayout';
import { DomainValidationNotice } from '@/components/ui/domain-validation-notice';
import { CertificateErrorNotice } from '@/components/ui/certificate-error-notice';
import { Input } from '@/components/ui/input';
import { Button as UiButton } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { LogOut, CheckCircle, Clock } from 'lucide-react';
import { env } from '@/env';
import { LoadingSpinner } from '@/components/layout/LoadingSpinner';
import { useRouter } from 'next/navigation';

export default function SetupPage() {
  const [domain, setDomain] = useState('');
  const [sslEnabled, setSslEnabled] = useState(true);
  const [forceSsl, setForceSsl] = useState(true);
  const [domainError, setDomainError] = useState<DomainError | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  
  const { logout } = useAuth();
  const router = useRouter();

  const { data: statusData, loading: statusLoading } = useQuery<{ status: PlatformStatus }>(GET_STATUS);
  const { data: settingsData, loading: settingsLoading } = useQuery<{ platformSettings: PlatformSetting }>(PLATFORM_SETTINGS, {
    skip: !statusData?.status?.domainInitialized
  });

  const REDIRECT_DELAY_MS = 5000;

  const [setupDomain, { loading }] = useMutation(SETUP_DOMAIN, {
    onCompleted: () => {
      setIsProcessing(false);
      setCountdown(REDIRECT_DELAY_MS / 1000);
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

  useEffect(() => {
    if (statusData?.status?.domainInitialized && settingsData?.platformSettings?.fqdn) {
      const { fqdn, sslEnabled: currentSslEnabled } = settingsData.platformSettings;
      const protocol = currentSslEnabled ? 'https://' : 'http://';
      
      if (window.location.hostname === fqdn) {
        router.push('/');
      } else {
        // Delay the redirect to allow the ingress to finish provisioning
        setTimeout(() => {
          window.location.href = `${protocol}${fqdn}`;
        }, REDIRECT_DELAY_MS);
      }
    }
  }, [statusData, settingsData, router]);

  useEffect(() => {
    if (countdown === null) return;
    if (countdown <= 0) {
      const protocol = sslEnabled ? 'https://' : 'http://';
      window.location.href = `${protocol}${domain}`;
      return;
    }
    const timer = setTimeout(() => setCountdown(prev => (prev !== null ? prev - 1 : null)), 1000);
    return () => clearTimeout(timer);
  }, [countdown, sslEnabled, domain]);

  if (statusLoading || (statusData?.status?.domainInitialized && settingsLoading)) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <LoadingSpinner size="lg" />
          <p className="mt-4 text-muted-foreground">Checking setup status...</p>
        </div>
      </div>
    );
  }

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

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Setup Header with Logout */}
      <header className="bg-card border-b border-border px-4 sm:px-6 py-4">
        <div className="flex items-center justify-between max-w-4xl mx-auto">
          <h1 className="text-lg sm:text-xl font-semibold text-foreground">
            {env.NEXT_PUBLIC_APP_NAME} Setup
          </h1>
          <UiButton
            variant="ghost"
            onClick={handleLogout}
            className="gap-2"
            size="sm"
          >
            <LogOut className="h-4 w-4" />
            <span className="hidden sm:inline">Sign Out</span>
          </UiButton>
        </div>
      </header>

      {/* Setup Content */}
      <div className="max-w-xl mx-auto px-4 sm:px-6 py-6 sm:py-8">
        <PageHeader 
          title="Platform Setup" 
          description="Configure your custom domain to complete the setup" 
        />
        
        <form onSubmit={handleSubmit} className="space-y-6 mt-6">
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
          
          {/* Domain Error Section */}
          {domainError && (
            <>
              {domainError.code === ErrorCode.DOMAIN_VALIDATION_ERROR && (
                <DomainValidationNotice
                  onContinueAnyway={handleContinueAnyway}
                  onTryAgain={() => setDomainError(null)}
                  isLoading={loading || isProcessing}
                />
              )}
              {domainError.code === ErrorCode.CERTIFICATE_ISSUANCE_ERROR && (
                <CertificateErrorNotice
                  onTryAgain={() => setDomainError(null)}
                  isLoading={loading || isProcessing}
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
            <Label htmlFor="sslEnabled" className="text-sm">Enable SSL (HTTPS)</Label>
          </div>
          
          <div className="flex items-center space-x-2">
            <Checkbox
              id="forceSsl"
              checked={forceSsl}
              onCheckedChange={(checked) => setForceSsl(Boolean(checked))}
              disabled={!sslEnabled}
            />
            <Label htmlFor="forceSsl" className="text-sm">Force HTTPS (Redirect all HTTP traffic to HTTPS)</Label>
          </div>
          
          {/* Processing Status */}
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
          
          <UiButton type="submit" disabled={loading || isProcessing || countdown !== null} className="w-full">
            {isProcessing ? (
              <>
                <Clock className="h-4 w-4 mr-2 animate-spin" />
                <span className="hidden sm:inline">
                  {sslEnabled ? 'Processing SSL Certificate...' : 'Configuring Domain...'}
                </span>
                <span className="sm:hidden">
                  {sslEnabled ? 'Processing...' : 'Configuring...'}
                </span>
              </>
            ) : loading ? (
              <>
                <Clock className="h-4 w-4 mr-2 animate-spin" />
                Validating...
              </>
            ) : (
              <>
                <CheckCircle className="h-4 w-4 mr-2" />
                Complete Setup
              </>
            )}
          </UiButton>
          {countdown !== null && (
            <p className="text-center text-sm text-muted-foreground mt-2">
              Redirecting in {countdown}...
            </p>
          )}
        </form>
        
        {/* Help Section */}
        <div className="mt-8 p-4 bg-muted/50 rounded-md">
          <h3 className="text-sm font-medium text-foreground mb-2">Need Help?</h3>
          <p className="text-sm text-muted-foreground">
            If you&apos;re having trouble with domain setup, make sure your domain&apos;s A record points to this server&apos;s IP address. 
            DNS changes can take up to 24 hours to propagate worldwide.
          </p>
        </div>
      </div>
    </div>
  );
} 