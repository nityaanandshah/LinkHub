import { useState, useCallback, useContext, createContext, ReactNode, useMemo } from 'react';
import React from 'react';
import apiClient from '../api/client';
import type { UserInfo, AuthResponse } from '../types/api';

interface AuthContextType {
  user: UserInfo | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<AuthResponse>;
  register: (email: string, password: string, displayName?: string) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  handleOAuthCallback: (accessToken: string, refreshToken: string) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  const [token, setToken] = useState<string | null>(() => localStorage.getItem('accessToken'));

  const isAuthenticated = !!token;

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<AuthResponse>('/auth/login', { email, password });
    const data = response.data;

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data.user));
    setToken(data.accessToken);
    setUser(data.user);

    return data;
  }, []);

  const register = useCallback(async (email: string, password: string, displayName?: string) => {
    const response = await apiClient.post<AuthResponse>('/auth/register', {
      email,
      password,
      displayName,
    });
    const data = response.data;

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data.user));
    setToken(data.accessToken);
    setUser(data.user);

    return data;
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Ignore logout errors
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }, []);

  const handleOAuthCallback = useCallback((accessToken: string, refreshToken: string) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    setToken(accessToken);

    // Decode basic user info from JWT payload (sub = userId)
    try {
      const payload = JSON.parse(atob(accessToken.split('.')[1]));
      const userInfo: UserInfo = {
        id: payload.sub ? parseInt(payload.sub) : 0,
        email: payload.email || '',
        displayName: payload.displayName || payload.email || '',
        role: payload.role || 'USER',
      };
      localStorage.setItem('user', JSON.stringify(userInfo));
      setUser(userInfo);
    } catch {
      // If JWT decode fails, just store the token — user info will be fetched later
    }
  }, []);

  const value = useMemo(
    () => ({ user, isAuthenticated, login, register, logout, handleOAuthCallback }),
    [user, isAuthenticated, login, register, logout, handleOAuthCallback]
  );

  return React.createElement(AuthContext.Provider, { value }, children);
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
