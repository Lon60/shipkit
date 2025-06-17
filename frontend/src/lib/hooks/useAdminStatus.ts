import { useQuery } from '@apollo/client';
import { GET_STATUS, type Status } from '@/lib/graphql';

export function useAdminStatus() {
  const { data, loading, error } = useQuery<{ status: Status }>(GET_STATUS);

  return {
    adminInitialized: data?.status?.adminInitialized ?? false,
    loading,
    error,
  };
} 