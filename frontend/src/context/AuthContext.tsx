import { createContext, useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import type { UserProfile } from '../types';

export type AuthContextValue = {
  user: UserProfile | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<UserProfile>;
  register: (fullName: string, email: string, password: string) => Promise<UserProfile>;
  logout: () => void;
  refresh: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const hasStoredToken = Boolean(localStorage.getItem('cineverse_token'));
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(hasStoredToken);

  const refresh = async () => {
    if (!localStorage.getItem('cineverse_token')) {
      setUser(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      setUser(await api<UserProfile>('/me'));
    } catch {
      localStorage.removeItem('cineverse_token');
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!hasStoredToken) return;

    let active = true;
    void api<UserProfile>('/me')
      .then((profile) => {
        if (active) setUser(profile);
      })
      .catch(() => {
        localStorage.removeItem('cineverse_token');
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [hasStoredToken]);

  const login = async (email: string, password: string) => {
    const response = await api<{ accessToken: string; user: UserProfile }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    localStorage.setItem('cineverse_token', response.accessToken);
    setUser(response.user);
    return response.user;
  };

  const register = async (fullName: string, email: string, password: string) => {
    const response = await api<{ accessToken: string; user: UserProfile }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, password }),
    });
    localStorage.setItem('cineverse_token', response.accessToken);
    setUser(response.user);
    return response.user;
  };

  const logout = () => {
    localStorage.removeItem('cineverse_token');
    setUser(null);
  };

  const value = useMemo(
    () => ({ user, loading, login, register, logout, refresh }),
    [user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
