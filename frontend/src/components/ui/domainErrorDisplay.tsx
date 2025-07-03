'use client';

import { AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { type DomainError } from '@/lib/utils';

interface DomainErrorDisplayProps {
  error: DomainError;
  onContinueAnyway?: () => void;
  onTryAgain?: () => void;
  isLoading?: boolean;
}

export function DomainErrorDisplay({
  error,
  onContinueAnyway,
  onTryAgain,
  isLoading = false,
}: DomainErrorDisplayProps) {
  return (
    <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-md">
      <div className="flex gap-3">
        <AlertCircle className="h-5 w-5 text-destructive mt-0.5 flex-shrink-0" />
        <div className="space-y-2">
          <p className="text-sm font-medium text-destructive">Domain Error</p>
          <p className="text-sm text-muted-foreground">{error.message}</p>
          {error.showContinueAnyway && (
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
            {error.showContinueAnyway && onContinueAnyway && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={onContinueAnyway}
                disabled={isLoading}
              >
                Continue Anyway
              </Button>
            )}
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