import { createContext, useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import type { UserProfile } from '../types';

export type RegisterPayload = {
    fullName: string;
    username: string;
    password: string;
    confirmPassword: string;
    phone: string;
};

export type AuthContextValue = {
    user: UserProfile | null;
    loading: boolean;
    login: (identifier: string, password: string) => Promise<UserProfile>;
    register: (payload: RegisterPayload) => Promise<UserProfile>;
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

    const login = async (identifier: string, password: string) => {
        const response = await api<{ accessToken: string; user: UserProfile }>('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ identifier, password }),
        });
        localStorage.setItem('cineverse_token', response.accessToken);
        setUser(response.user);
        return response.user;
    };

    const register = async (payload: RegisterPayload) => {
        const response = await api<{ accessToken: string; user: UserProfile }>('/auth/register', {
            method: 'POST',
            body: JSON.stringify(payload),
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
