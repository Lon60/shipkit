'use client';

import { DashboardLayout } from '@/components/dashboard/layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Settings as SettingsIcon, Clock } from 'lucide-react';

export default function SettingsPage() {
  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
            <SettingsIcon className="h-6 w-6" />
            Settings
          </h1>
          <p className="text-muted-foreground mt-1">
            Manage your Shipkit preferences and configuration
          </p>
        </div>

        <Card className="border-border bg-card">
          <CardHeader className="text-center">
            <div className="mx-auto w-12 h-12 bg-muted rounded-full flex items-center justify-center mb-4">
              <Clock className="h-6 w-6 text-muted-foreground" />
            </div>
            <CardTitle className="text-foreground">Coming Soon</CardTitle>
            <CardDescription className="text-muted-foreground">
              Settings and configuration options are currently under development
            </CardDescription>
          </CardHeader>
          <CardContent className="text-center">
            <p className="text-sm text-muted-foreground">
              We&apos;re working hard to bring you comprehensive settings to customize your Shipkit experience. 
              Stay tuned for updates!
            </p>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
} 