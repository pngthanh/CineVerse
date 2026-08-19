import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { api } from '../lib/api';
import type { Booking, SeatHold } from '../types';

type ResumeTarget = {
    path: '/checkout' | '/payment';
    label: string;
} | null;

function readLocalResumeTarget(): ResumeTarget {
    try {
        const booking = JSON.parse(sessionStorage.getItem('cineverse_booking') || 'null') as Booking | null;
        if (booking?.status === 'PENDING' && new Date(booking.expiresAt).getTime() > Date.now()) {
            return { path: '/payment', label: 'Tiếp tục thanh toán' };
        }

        const hold = JSON.parse(sessionStorage.getItem('cineverse_hold') || 'null') as SeatHold | null;
        if (hold && new Date(hold.expiresAt).getTime() > Date.now()) {
            return { path: '/checkout', label: 'Quay lại xác nhận' };
        }
    }
    catch {
        sessionStorage.removeItem('cineverse_booking');
        sessionStorage.removeItem('cineverse_hold');
    }

    return null;
}

export function CustomerHeader() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [resumeTarget, setResumeTarget] = useState<ResumeTarget>(null);

    useEffect(() => {

        if (!user) return;

        let cancelled = false;
        void api<Booking[]>('/bookings')
            .then((bookings) => {
                if (cancelled) return;
                const pending = bookings
                    .filter((booking) => booking.status === 'PENDING' && new Date(booking.expiresAt).getTime() > Date.now())
                    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0];

                if (pending) {
                    sessionStorage.setItem('cineverse_booking', JSON.stringify(pending));
                    setResumeTarget({ path: '/payment', label: 'Tiếp tục thanh toán' });
                    return;
                }

                sessionStorage.removeItem('cineverse_booking');
                setResumeTarget(readLocalResumeTarget());
            })
            .catch(() => setResumeTarget(readLocalResumeTarget()));

        return () => {
            cancelled = true;
        };
    }, [location.pathname, user]);

    const handleLogout = () => {
        logout();
        sessionStorage.removeItem('cineverse_booking');
        sessionStorage.removeItem('cineverse_hold');
        setResumeTarget(null);
        navigate('/');
    };

    return (
        <header className="site-header">
            <div className="container nav">
                <Link className="brand brand-lockup" to="/">
                    <span>CineVerse</span>
                </Link>

                <nav aria-label="Điều hướng chính">
                    <NavLink to="/">Trang chủ</NavLink>
                    <NavLink to="/movies">Phim</NavLink>
                    <NavLink to="/cinemas">Rạp</NavLink>
                </nav>

                {user && resumeTarget && location.pathname !== resumeTarget.path && (
                    <div className="nav-resume-slot">
                        <button className="nav-resume" type="button" onClick={() => navigate(resumeTarget.path)}>
                            <span className="nav-resume-dot" aria-hidden="true" />
                            {resumeTarget.label}
                        </button>
                    </div>
                )}

                <div className="nav-actions">
                    {user?.role === 'ADMIN' && <NavLink className="nav-admin" to="/admin">Quản trị</NavLink>}
                    <NavLink className="nav-ticket" to="/bookings">Vé của tôi</NavLink>
                    {user ? (
                        <>
                            <NavLink to="/account">{user.fullName}</NavLink>
                            <button className="link-button" type="button" onClick={handleLogout}>Đăng xuất</button>
                        </>
                    ) : <NavLink className="btn btn-sm" to="/login">Đăng nhập</NavLink>}
                </div>
            </div>
        </header>
    );
}