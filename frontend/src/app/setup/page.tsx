'use client';

import { useState } from 'react';
import { useMutation } from '@apollo/client';
import { SETUP_DOMAIN } from '@/lib/graphql';
import { useAuth } from '@/lib/auth';
import { PageHeader } from '@/components/layout/PageLayout';
import { Input } from '@/components/ui/input';
import { Button as UiButton } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { LogOut, AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { env } from '@/env';

export default function SetupPage() {
  const [domain, setDomain] = useState('');
  const [sslEnabled, setSslEnabled] = useState(true);
  const [forceSsl, setForceSsl] = useState(true);
  const [domainError, setDomainError] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  
  const { logout } = useAuth();

  const [setupDomain, { loading }] = useMutation(SETUP_DOMAIN, {
    onCompleted: () => {
      const protocol = sslEnabled ? 'https://' : 'http://';
      toast.success('Domain configured successfully! Redirecting...');
      setIsProcessing(false);
      window.location.href = `${protocol}${domain}`;
    },
    onError: (err) => {
      setIsProcessing(false);
      if (err.message.includes('Failed to resolve domain')) {
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

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Setup Header with Logout */}
      <header className="bg-card border-b border-border px-6 py-4">
        <div className="flex items-center justify-between max-w-4xl mx-auto">
          <h1 className="text-xl font-semibold text-foreground">
            {env.NEXT_PUBLIC_APP_NAME} Setup
          </h1>
          <UiButton
            variant="ghost"
            onClick={handleLogout}
            className="gap-2"
          >
            <LogOut className="h-4 w-4" />
            Sign Out
          </UiButton>
        </div>
      </header>

      {/* Setup Content */}
      <div className="max-w-xl mx-auto px-6 py-8">
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
            <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-md">
              <div className="flex gap-3">
                <AlertCircle className="h-5 w-5 text-destructive mt-0.5 flex-shrink-0" />
                <div className="space-y-2">
                  <p className="text-sm font-medium text-destructive">Domain Setup Required</p>
                  <p className="text-sm text-muted-foreground">{domainError}</p>
                  {(domainError.includes('Domain resolution failed')) && (
                    <div className="space-y-2 text-sm text-muted-foreground">
                      <p><strong>To fix this:</strong></p>
                      <ol className="list-decimal list-inside space-y-1 ml-2">
                        <li>Go to your domain registrar&apos;s DNS management</li>
                        <li>Create an A record for your domain</li>
                        <li>Point it to your server&apos;s IP address</li>
                        <li>Wait for DNS propagation (can take up to 24 hours)</li>
                      </ol>
                    </div>
                  )}
                  <div className="flex gap-2 mt-3">
                    {(domainError.includes('Domain resolution failed')) && (
                      <UiButton
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={handleContinueAnyway}
                        disabled={loading || isProcessing}
                      >
                        Continue Anyway
                      </UiButton>
                    )}
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
            <Label htmlFor="forceSsl">Force HTTPS (Redirect all HTTP traffic to HTTPS)</Label>
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
          
          <UiButton type="submit" disabled={loading || isProcessing} className="w-full">
            {isProcessing ? (
              <>
                <Clock className="h-4 w-4 mr-2 animate-spin" />
                {sslEnabled ? 'Processing SSL Certificate...' : 'Configuring Domain...'}
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