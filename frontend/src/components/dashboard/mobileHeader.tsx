'use client';

import { useState } from 'react';
import { useAuth } from '@/lib/auth';
import { useRouter, usePathname } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { 
  Menu,
  LayoutDashboard, 
  Settings, 
  LogOut, 
  ExternalLink,
  Plus,
  User
} from 'lucide-react';
import { env } from '@/env';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';

interface MobileHeaderProps {
  onCreateDeployment?: () => void;
}

export function MobileHeader({ onCreateDeployment }: MobileHeaderProps) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setIsOpen(false);
  };

  const handleNavigation = (path: string) => {
    router.push(path);
    setIsOpen(false);
  };

  const handleCreateDeployment = () => {
    onCreateDeployment?.();
    setIsOpen(false);
  };

  return (
    <header className="md:hidden bg-card border-b border-border px-4 py-3">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-foreground">
          {env.NEXT_PUBLIC_APP_NAME}
        </h1>
        
        <div className="flex items-center">
          <Sheet open={isOpen} onOpenChange={setIsOpen}>
            <SheetTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="p-2"
              >
                <Menu className="h-5 w-5" />
                <span className="sr-only">Open navigation menu</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="w-80 p-0">
              <div className="flex h-full flex-col">
                {/* Header */}
                <SheetHeader className="p-6 pb-4">
                  <SheetTitle className="text-left">
                    {env.NEXT_PUBLIC_APP_NAME}
                  </SheetTitle>
                  <SheetDescription className="text-left">
                    Navigate through your deployment platform
                  </SheetDescription>
                </SheetHeader>

                {/* User Info */}
                <div className="px-6 pb-4">
                  <div className="flex items-center space-x-3 p-3 bg-accent/50 rounded-lg">
                    <div className="bg-primary/10 p-2 rounded-full">
                      <User className="h-4 w-4 text-primary" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">
                        {user?.email}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        Administrator
                      </p>
                    </div>
                  </div>
                </div>

                {/* Navigation */}
                <nav className="flex-1 px-6 space-y-2">
                  {/* Create Deployment Button */}
                  <Button 
                    onClick={handleCreateDeployment} 
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
                    onClick={() => handleNavigation("/")}
                  >
                    <LayoutDashboard className="h-4 w-4" />
                    Deployments
                  </Button>
                </nav>

                {/* Footer */}
                <div className="px-6 py-6 border-t border-border space-y-4">
                  {/* Settings Navigation */}
                  <Button
                    variant={pathname === "/settings" ? "secondary" : "ghost"}
                    className={cn(
                      "w-full justify-start gap-2",
                      pathname === "/settings" 
                        ? "bg-secondary text-secondary-foreground" 
                        : "text-muted-foreground hover:text-foreground hover:bg-accent"
                    )}
                    onClick={() => handleNavigation("/settings")}
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

                  <div className="space-y-2">
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
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
} 