import { useQuery } from '@apollo/client';
import { GET_STATUS, type PlatformStatus } from '@/lib/graphql';

export function usePlatformStatus() {
  const { data, loading, error } = useQuery<{ status: PlatformStatus }>(GET_STATUS);

  return {
    adminInitialized: data?.status?.adminInitialized ?? false,
    domainInitialized: data?.status?.domainInitialized ?? false,
    loading,
    error,
  };
} 

export { usePlatformStatus as useAdminStatus }; 