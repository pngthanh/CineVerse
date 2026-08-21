import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function StaffLayout() {
    const { user, logout } = useAuth();
    return (
        <div className="staff-shell">
            <aside className="staff-sidebar">
                <div className="staff-brand">CineVerse <span>STAFF</span></div>
                <div className="staff-assignment">
                    <small>Rạp được phân công</small>
                    <strong>{user?.assignedCinemaName ?? 'Chưa phân công'}</strong>
                </div>
                <nav>
                    <NavLink to="/staff" end>Kiểm tra vé</NavLink>
                    <NavLink to="/">Về trang khách</NavLink>
                </nav>
                <button className="btn btn-secondary btn-block" onClick={logout}>Đăng xuất</button>
            </aside>
            <main className="staff-content"><Outlet /></main>
        </div>
    );
}
