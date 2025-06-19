'use client';

import { useAuth } from '@/lib/auth';
import { Button } from '@/components/ui/button';
import { LogOut, Plus } from 'lucide-react';
import { env } from '@/env';

interface MobileHeaderProps {
  onCreateDeployment?: () => void;
}

export function MobileHeader({ onCreateDeployment }: MobileHeaderProps) {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <header className="md:hidden bg-card border-b border-border px-4 py-3">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-foreground">
          {env.NEXT_PUBLIC_APP_NAME}
        </h1>
        
        <div className="flex items-center space-x-2">
          <Button 
            onClick={onCreateDeployment} 
            size="sm"
            className="gap-1"
          >
            <Plus className="h-4 w-4" />
            Deploy
          </Button>
          
          <Button
            variant="ghost"
            size="sm"
            onClick={handleLogout}
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>
      
      <div className="mt-2 text-xs text-muted-foreground">
        {user?.email}
      </div>
    </header>
  );
} 