import { ApolloClient, InMemoryCache, createHttpLink, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import { trpcClient } from '@/utils/trpc';

// JWT utility function to check if token is expired
interface JWTPayload {
  sub?: string;
  exp?: number;
  iat?: number;
  roles?: string[];
  [key: string]: unknown;
}

const isTokenExpired = (token: string): boolean => {
  try {
    const parts = token.split('.');
    if (parts.length !== 3 || !parts[1]) {
      return true;
    }
    
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    
    const decoded = JSON.parse(jsonPayload) as JWTPayload;
    if (!decoded.exp) {
      return true;
    }
    
    const currentTime = Math.floor(Date.now() / 1000);
    return decoded.exp < currentTime;
  } catch (error) {
    console.error('Error checking JWT expiration:', error);
    return true;
  }
};

const clearExpiredToken = () => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUser');
  }
};

// Get config from tRPC
const getConfig = async () => {
  try {
    const config = await trpcClient.config.query();
    return config;
  } catch (error) {
    console.warn('Failed to fetch config via tRPC, using fallback', error);
    return {
      apiBaseUrl: '/api',
      appName: 'Shipkit',
    };
  }
};

// Initialize with fallback, will be updated after config is fetched
let currentApiBaseUrl = '/api';

// Create HTTP link that uses the current URL
const httpLink = createHttpLink({
  uri: () => `${currentApiBaseUrl}/graphql`,
});

const authLink = setContext((_, { headers }) => {
  const token = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null;
  
  if (token && isTokenExpired(token)) {
    console.log('Token expired in Apollo client, clearing token...');
    clearExpiredToken();
    return {
      headers: {
        ...(headers as Record<string, unknown>),
        authorization: '',
      },
    };
  }
  
  return {
    headers: {
      ...(headers as Record<string, unknown>),
      authorization: token ? `Bearer ${token}` : '',
    },
  };
});

const errorLink = onError(({ graphQLErrors, networkError }) => {
  if (graphQLErrors) {
          graphQLErrors.forEach(({ message, locations, path }) => {
        console.error(
          `GraphQL error: Message: ${message}, Location: ${JSON.stringify(locations)}, Path: ${JSON.stringify(path)}`
        );
      
      if (message.includes('Access Denied') || message.includes('Unauthorized') || message.includes('jwt expired')) {
        clearExpiredToken();
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
      }
    });
  }

  if (networkError) {
    console.error(`Network error: ${networkError}`);
    
    if ('statusCode' in networkError && networkError.statusCode === 401) {
      clearExpiredToken();
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }
  }
});

const initializeConfig = async () => {
  if (typeof window !== 'undefined') {
    try {
      const config = await getConfig();
      currentApiBaseUrl = config.apiBaseUrl;
    } catch (error) {
      console.warn('Failed to initialize config, using fallback', error);
    }
  }
};

// Initialize immediately for client-side
if (typeof window !== 'undefined') {
  void initializeConfig();
}

export const apolloClient = new ApolloClient({
  link: from([errorLink, authLink, httpLink]),
  cache: new InMemoryCache(),
  defaultOptions: {
    watchQuery: {
      errorPolicy: 'all',
    },
    query: {
      errorPolicy: 'all',
    },
  },
}); 