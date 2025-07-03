'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useMutation } from '@apollo/client';
import { LOGIN_MUTATION, REGISTER_MUTATION, type AuthPayload, type CreateAccountInput, type Account } from './graphql';

interface User {
  id?: string;
  email: string;
  authorities: string[];
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (input: CreateAccountInput) => Promise<void>;
  logout: () => void;
  updateUser: (account: Account) => void;
}

interface JWTPayload {
  sub?: string;
  exp?: number;
  iat?: number;
  roles?: string[];
  [key: string]: unknown;
}

const decodeJWT = (token: string): JWTPayload | null => {
  try {
    const parts = token.split('.');
    if (parts.length !== 3 || !parts[1]) {
      return null;
    }
    
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload) as JWTPayload;
  } catch (error) {
    console.error('Error decoding JWT:', error);
    return null;
  }
};

const isTokenExpired = (token: string): boolean => {
  const decoded = decodeJWT(token);
  if (!decoded?.exp) {
    return true;
  }
  
  const currentTime = Math.floor(Date.now() / 1000);
  return decoded.exp < currentTime;
};

const getTokenExpirationTime = (token: string): number | null => {
  const decoded = decodeJWT(token);
  return decoded?.exp ?? null;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const [loginMutation] = useMutation<{ login: AuthPayload }>(LOGIN_MUTATION);
  const [registerMutation] = useMutation<{ register: AuthPayload }>(REGISTER_MUTATION);

  const logout = useCallback((): void => {
    setToken(null);
    setUser(null);
    
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUser');
  }, []);

  const updateUser = useCallback((account: Account): void => {
    setUser((prev) => {
      const updated: User = { ...(prev ?? {}), ...account } as User;
      localStorage.setItem('authUser', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const checkTokenExpiration = useCallback((): boolean => {
    const savedToken = localStorage.getItem('authToken');
    
    if (!savedToken) {
      return false;
    }
    
    if (isTokenExpired(savedToken)) {
      console.log('Token expired, logging out...');
      logout();
      return false;
    }
    
    return true;
  }, [logout]);

  useEffect(() => {
    const savedToken = localStorage.getItem('authToken');
    const savedUser = localStorage.getItem('authUser');
    
    if (savedToken && savedUser) {
      if (!isTokenExpired(savedToken)) {
        setToken(savedToken);
        setUser(JSON.parse(savedUser) as User);
      } else {
        logout();
      }
    }
    
    setIsLoading(false);
  }, [logout]);

  useEffect(() => {
    if (!token) return;

    const intervalId = setInterval(() => {
      checkTokenExpiration();
    }, 60000);

    const expirationTime = getTokenExpirationTime(token);
    if (expirationTime) {
      const currentTime = Math.floor(Date.now() / 1000);
      const timeUntilExpiration = (expirationTime - currentTime) * 1000;
      
      if (timeUntilExpiration > 0) {
        const timeoutId = setTimeout(() => {
          console.log('Token expired at exact time, logging out...');
          logout();
        }, timeUntilExpiration);

        return () => {
          clearInterval(intervalId);
          clearTimeout(timeoutId);
        };
      }
    }

    return () => clearInterval(intervalId);
  }, [token, checkTokenExpiration, logout]);

  useEffect(() => {
    const handleFocus = () => {
      if (token) {
        checkTokenExpiration();
      }
    };

    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [token, checkTokenExpiration]);

  const login = async (email: string, password: string): Promise<void> => {
    try {
      const { data } = await loginMutation({
        variables: { email, password },
      });

      if (data?.login?.token && data.login.account) {
        const authToken = data.login.token;
        
        if (isTokenExpired(authToken)) {
          throw new Error('Received expired token');
        }
        
        const accountData = data.login.account;
        const decoded = decodeJWT(authToken);
        const userId = typeof decoded?.sub === 'string' ? decoded.sub : undefined;

        const userData: User = { id: userId, ...accountData };

        setToken(authToken);
        setUser(userData);
        
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('authUser', JSON.stringify(userData));
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const register = async (input: CreateAccountInput): Promise<void> => {
    try {
      const { data } = await registerMutation({
        variables: { input },
      });

      if (data?.register?.token && data.register.account) {
        const authToken = data.register.token;
        
        if (isTokenExpired(authToken)) {
          throw new Error('Received expired token');
        }
        
        const accountData = data.register.account;
        const decoded = decodeJWT(authToken);
        const userId = typeof decoded?.sub === 'string' ? decoded.sub : undefined;

        const userData: User = { id: userId, ...accountData };

        setToken(authToken);
        setUser(userData);
        
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('authUser', JSON.stringify(userData));
      }
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  };

  const isAuthenticated = !!token && !!user;

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout,
    updateUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
} 