'use client';

import { AccountInfo } from './AccountInfo';
import { DomainConfig } from './DomainConfig';

export function AccountSettingsForm() {
  return (
    <div className="space-y-6">
      <AccountInfo />
      <DomainConfig />
    </div>
  );
} 