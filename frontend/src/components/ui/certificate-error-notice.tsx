'use client';

import { AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface CertificateErrorNoticeProps {
  onTryAgain?: () => void;
  isLoading?: boolean;
}

export function CertificateErrorNotice({
  onTryAgain,
  isLoading = false,
}: CertificateErrorNoticeProps) {
  return (
    <div className="p-4 bg-orange-50 dark:bg-orange-950/50 border border-orange-200 dark:border-orange-800 rounded-md">
      <div className="flex gap-3">
        <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
        <div className="space-y-2">
          <p className="text-sm font-medium text-orange-700 dark:text-orange-400">SSL Certificate Error</p>
          <p className="text-sm text-muted-foreground">Unable to issue SSL certificate. Please check your domain configuration.</p>
          <div className="flex gap-2 mt-3">
            {onTryAgain && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={onTryAgain}
                disabled={isLoading}
              >
                Try Again
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
} 