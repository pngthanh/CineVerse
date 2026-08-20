import { NavLink, Outlet } from 'react-router-dom';

export function AdminLayout() {
    return (
        <div className="admin-shell">
            <aside className="admin-sidebar">
                <div className="admin-sidebar-top">
                    <div className="brand">CineVerse</div>
                    <small>QUẢN TRỊ</small>
                    <nav aria-label="Điều hướng quản trị">
                        <NavLink end to="/admin">Tổng quan</NavLink>
                        <NavLink to="/admin/movies">Phim</NavLink>
                        <NavLink to="/admin/cinemas">Rạp & phòng</NavLink>
                        <NavLink to="/admin/showtimes">Suất chiếu</NavLink>
                        <NavLink to="/admin/bookings">Đặt vé</NavLink>
                        <NavLink to="/admin/users">Người dùng</NavLink>
                        <NavLink to="/admin/vouchers">Voucher</NavLink>
                        <NavLink to="/admin/concessions">Bắp nước & combo</NavLink>
                    </nav>
                </div>
                <NavLink className="admin-back-customer" to="/">Về trang khách</NavLink>
            </aside>
            <section className="admin-content"><Outlet /></section>
        </div>
    );
}
