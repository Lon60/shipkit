'use client';

import { useEffect } from 'react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { User } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { useQuery } from '@apollo/client';
import { GET_ACCOUNT, type Account } from '@/lib/graphql';

export function AccountInfo() {
  const { user, updateUser } = useAuth();

  const { data } = useQuery<{ account: Account }>(GET_ACCOUNT, {
    variables: { id: user?.id },
    skip: !user?.id,
    fetchPolicy: 'network-only',
  });

  useEffect(() => {
    if (data?.account) {
      updateUser(data.account);
    }
  }, [data, updateUser]);

  const emailValue = data?.account?.email ?? user?.email ?? '';

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center space-x-2">
          <User className="h-5 w-5" />
          <CardTitle>Account Information</CardTitle>
        </div>
        <CardDescription>
          Your basic account details
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-medium">Email</Label>
            <Input
              id="email"
              value={emailValue}
              disabled
              className="bg-muted text-muted-foreground"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="role" className="text-sm font-medium">Role</Label>
            <Input
              id="role"
              value="Administrator"
              disabled
              className="bg-muted text-muted-foreground"
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
} 