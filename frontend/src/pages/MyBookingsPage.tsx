import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { StatusBadge } from '../components/StatusBadge';
import { ApiError, api } from '../lib/api';
import { dateTime, money } from '../lib/format';
import type { Booking } from '../types';

type TicketTab = 'ACTIVE' | 'USED' | 'INACTIVE';

export function MyBookingsPage() {
    const [items, setItems] = useState<Booking[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [workingId, setWorkingId] = useState<number | null>(null);
    const [tab, setTab] = useState<TicketTab>('ACTIVE');
    const navigate = useNavigate();
    const load = () => api<Booking[]>('/bookings').then(setItems);
    useEffect(() => { void load().catch(() => setError('Không thể tải danh sách vé.')).finally(() => setLoading(false)); }, []);

    const filtered = useMemo(() => items.filter((booking) => {
        if (tab === 'USED') return booking.ticketStatus === 'USED';
        if (tab === 'INACTIVE') return booking.status === 'CANCELLED' || booking.ticketStatus === 'CANCELLED';
        return booking.ticketStatus !== 'USED' && booking.status !== 'CANCELLED' && booking.ticketStatus !== 'CANCELLED';
    }), [items, tab]);

    const resume = (booking: Booking) => { sessionStorage.setItem('cineverse_booking', JSON.stringify(booking)); navigate('/payment'); };
    const cancel = async (booking: Booking) => {
        setWorkingId(booking.id);
        try { await api(`/bookings/${booking.id}/cancel`, { method: 'POST' }); await load(); }
        catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể hủy booking.'); }
        finally { setWorkingId(null); }
    };

    return <div className="container page"><div className="page-title"><span className="eyebrow">BOOKING CENTER</span><h1>Vé của tôi</h1><p>Quản lý vé chưa sử dụng, vé đã check-in và booking đã hủy.</p></div>
        <div className="ticket-tabs"><button className={tab === 'ACTIVE' ? 'active' : ''} onClick={() => setTab('ACTIVE')}>Chưa sử dụng</button><button className={tab === 'USED' ? 'active' : ''} onClick={() => setTab('USED')}>Đã sử dụng</button><button className={tab === 'INACTIVE' ? 'active' : ''} onClick={() => setTab('INACTIVE')}>Đã hủy</button></div>
        {loading && <div className="empty">Đang tải...</div>}{error && <div className="alert alert-error">{error}</div>}
        <div className="booking-list">{filtered.map((booking) => <article className="booking-card" key={booking.id}><Link to={`/bookings/${booking.id}`} className="booking-main"><small>{booking.ticketCode ?? booking.bookingCode}</small><h3>{booking.showtime.movieTitle}</h3><p>{booking.showtime.cinemaName} · {dateTime(booking.showtime.startTime)}</p><p>Ghế {booking.seats.map((seat) => seat.code).join(', ')}</p></Link><div className="booking-side"><StatusBadge value={booking.ticketStatus ?? booking.status}/><strong>{money(booking.totalAmount)}</strong>{booking.status === 'PENDING' && <div className="booking-actions"><button className="btn btn-sm" onClick={() => resume(booking)}>Tiếp tục thanh toán</button><button className="btn btn-secondary btn-sm" disabled={workingId === booking.id} onClick={() => void cancel(booking)}>Hủy & trả ghế</button></div>}</div></article>)}</div>
        {!loading && !filtered.length && <div className="empty"><p>Không có vé trong mục này.</p><Link className="btn" to="/movies">Xem phim đang chiếu</Link></div>}
    </div>;
}
