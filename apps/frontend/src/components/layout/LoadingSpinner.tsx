import { cn } from '@/lib/utils';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function LoadingSpinner({ size = 'md', className }: LoadingSpinnerProps) {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-8 w-8',
    lg: 'h-12 w-12'
  };

  return (
    <div className={cn(
      "animate-spin rounded-full border-b-2 border-primary",
      sizeClasses[size],
      className
    )}></div>
  );
}

interface LoadingPageProps {
  message?: string;
  className?: string;
}

export function LoadingPage({ message = "Loading...", className }: LoadingPageProps) {
  return (
    <div className={cn("min-h-screen bg-background flex items-center justify-center", className)}>
      <div className="text-center">
        <LoadingSpinner size="lg" className="mx-auto" />
        <p className="mt-4 text-sm text-muted-foreground">{message}</p>
      </div>
    </div>
  );
}

interface LoadingContainerProps {
  children?: React.ReactNode;
  message?: string;
  className?: string;
}

export function LoadingContainer({ 
  children, 
  message = "Loading...", 
  className 
}: LoadingContainerProps) {
  return (
    <div className={cn("flex items-center justify-center p-8", className)}>
      <div className="text-center">
        <LoadingSpinner className="mx-auto" />
        <p className="mt-2 text-sm text-muted-foreground">{message}</p>
        {children}
      </div>
    </div>
  );
} 