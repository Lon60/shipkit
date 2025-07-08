'use client';

import { useAuth } from '@/lib/auth';
import { useRouter, usePathname } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { 
  LayoutDashboard, 
  Settings, 
  LogOut, 
  ExternalLink,
  Plus
} from 'lucide-react';
import { env } from '@/env';

interface SidebarProps {
  onCreateDeployment?: () => void;
}

export function Sidebar({ onCreateDeployment }: SidebarProps) {
  const { logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="hidden md:flex h-screen w-64 flex-col bg-card border-r border-border fixed left-0 top-0 z-50">
      {/* Header */}
      <div className="flex h-16 items-center px-6">
        <h1 className="text-xl font-semibold text-foreground">
          {env.NEXT_PUBLIC_APP_NAME}
        </h1>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-4 py-4">
        {/* Create Deployment Button */}
        <Button 
          onClick={onCreateDeployment} 
          className="w-full justify-start gap-2 mb-4"
        >
          <Plus className="h-4 w-4" />
          Deploy
        </Button>

        {/* Deployments Navigation */}
        <Button
          variant={pathname === "/" ? "secondary" : "ghost"}
          className={cn(
            "w-full justify-start gap-2",
            pathname === "/" 
              ? "bg-secondary text-secondary-foreground" 
              : "text-muted-foreground hover:text-foreground hover:bg-accent"
          )}
          onClick={() => router.push("/")}
        >
          <LayoutDashboard className="h-4 w-4" />
          Deployments
        </Button>
      </nav>

      {/* Middle Section - Settings and Sign Out */}
      <div className="px-4 py-4 border-t border-border space-y-1">
        <Button
          variant={pathname === "/settings" ? "secondary" : "ghost"}
          className={cn(
            "w-full justify-start gap-2",
            pathname === "/settings" 
              ? "bg-secondary text-secondary-foreground" 
              : "text-muted-foreground hover:text-foreground hover:bg-accent"
          )}
          onClick={() => router.push("/settings")}
        >
          <Settings className="h-4 w-4" />
          Settings
        </Button>

        <Button
          variant="ghost"
          className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground hover:bg-accent"
          onClick={handleLogout}
        >
          <LogOut className="h-4 w-4" />
          Sign Out
        </Button>
      </div>

      {/* Footer */}
      <div className="px-4 py-4 border-t border-border space-y-2">
        <div className="text-xs text-muted-foreground">
          Shipkit v{process.env.APP_VERSION}
        </div>
        <a
          href="https://github.com/Lon60/shipkit"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          <ExternalLink className="h-3 w-3" />
          GitHub Repo
        </a>
      </div>
    </div>
  );
} 