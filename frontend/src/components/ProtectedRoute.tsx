import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type { Role } from '../types';

export function ProtectedRoute({ admin = false, role }: { admin?: boolean; role?: Role }) {
    const { user, loading } = useAuth();
    if (loading) return <div className="page-center">Đang tải...</div>;
    if (!user) return <Navigate to="/login" replace />;
    const expectedRole = admin ? 'ADMIN' : role;
    if (expectedRole && user.role !== expectedRole) return <Navigate to="/403" replace />;
    return <Outlet />;
}
