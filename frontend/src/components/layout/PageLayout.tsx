import { cn } from '@/lib/utils';

interface PageLayoutProps {
  children: React.ReactNode;
  className?: string;
  containerClassName?: string;
  fullWidth?: boolean;
}

export function PageLayout({ 
  children, 
  className, 
  containerClassName,
  fullWidth = false 
}: PageLayoutProps) {
  return (
    <div className={cn("min-h-screen bg-background", className)}>
      <div className={cn(
        "mx-auto px-6 py-8",
        fullWidth ? "w-full" : "max-w-7xl",
        containerClassName
      )}>
        {children}
      </div>
    </div>
  );
}

interface PageHeaderProps {
  title: string;
  description?: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
  className?: string;
}

export function PageHeader({ 
  title, 
  description, 
  icon, 
  action, 
  className 
}: PageHeaderProps) {
  return (
    <div className={cn("mb-8", className)}>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
            {icon && icon}
            {title}
          </h1>
          {description && (
            <p className="text-muted-foreground mt-1">
              {description}
            </p>
          )}
        </div>
        {action && <div>{action}</div>}
      </div>
    </div>
  );
}

interface AuthLayoutProps {
  children: React.ReactNode;
  className?: string;
}

export function AuthLayout({ children, className }: AuthLayoutProps) {
  return (
    <div className={cn("min-h-screen bg-background", className)}>
      <div className="flex min-h-screen items-center justify-center p-8 lg:p-12">
        {children}
      </div>
    </div>
  );
}

interface SplitLayoutProps {
  leftContent: React.ReactNode;
  rightContent: React.ReactNode;
  className?: string;
  maxWidth?: string;
}

export function SplitLayout({ 
  leftContent, 
  rightContent, 
  className,
  maxWidth = "max-w-6xl"
}: SplitLayoutProps) {
  return (
    <AuthLayout className={className}>
      <div className={cn("flex w-full", maxWidth)}>
        {/* Left Side */}
        <div className="flex-1 flex items-center justify-end pr-8 lg:pr-12">
          {leftContent}
        </div>

        {/* Vertical Divider */}
        <div className="w-px bg-border self-stretch my-8"></div>

        {/* Right Side */}
        <div className="flex-1 flex items-center justify-start pl-8 lg:pl-12">
          {rightContent}
        </div>
      </div>
    </AuthLayout>
  );
} 