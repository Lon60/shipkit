'use client';

import { DashboardLayout } from '@/components/dashboard/layout';
import { PageHeader } from '@/components/layout/PageLayout';
import { GeneralSettingsForm } from '@/components/dashboard/GeneralSettingsForm';
import { DomainSettingsForm } from '@/components/dashboard/DomainSettingsForm';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Settings as SettingsIcon } from 'lucide-react';

export default function SettingsPage() {
  return (
    <DashboardLayout>
      <div className="max-w-5xl mx-auto px-6 py-8">
        <PageHeader 
          title="Settings"
          description="Manage your Shipkit preferences and configuration"
          icon={<SettingsIcon className="h-6 w-6" />}
        />

        <div className="mt-8">
          <Tabs defaultValue="general" className="w-full">
            <TabsList className="grid w-full grid-cols-2 lg:grid-cols-4 h-auto p-1">
              <TabsTrigger value="general" className="py-2">General</TabsTrigger>
              <TabsTrigger value="domain" className="py-2">Domain</TabsTrigger>
            </TabsList>
            <TabsContent value="general" className="mt-6">
              <Card>
                <CardHeader>
                  <CardTitle>General Settings</CardTitle>
                  <CardDescription>Manage your account password and other general preferences.</CardDescription>
                </CardHeader>
                <CardContent>
                  <GeneralSettingsForm />
                </CardContent>
              </Card>
            </TabsContent>
            <TabsContent value="domain" className="mt-6">
              <Card>
                <CardHeader>
                  <CardTitle>Domain Settings</CardTitle>
                  <CardDescription>Configure your custom domain and SSL settings.</CardDescription>
                </CardHeader>
                <CardContent>
                  <DomainSettingsForm />
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </DashboardLayout>
  );
}