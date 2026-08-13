import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { StatusBadge } from '../components/StatusBadge';
import { ApiError, api } from '../lib/api';
import { dateTime, money } from '../lib/format';
import type { Booking } from '../types';
export function MyBookingsPage() {
    const [items, setItems] = useState<Booking[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [workingId,setWorkingId]=useState<number|null>(null); const navigate=useNavigate();
    const load=()=>api<Booking[]>('/bookings').then(setItems);
    useEffect(()=>{void load().catch(()=>setError('Không thể tải danh sách vé.')).finally(()=>setLoading(false));},[]);
    const resume=(booking:Booking)=>{sessionStorage.setItem('cineverse_booking',JSON.stringify(booking)); navigate('/payment');};
    const cancel=async (booking:Booking)=>{setWorkingId(booking.id); try{await api(`/bookings/${booking.id}/cancel`,{method:'POST'}); await load();}catch(e){setError(e instanceof ApiError?e.message:'Không thể hủy booking.');}finally{setWorkingId(null);}};
    return <div className="container page"><div className="page-title"><span className="eyebrow">BOOKING CENTER</span><h1>Vé của tôi</h1><p>Quản lý vé đã đặt và tiếp tục các booking đang chờ thanh toán.</p></div>{loading&&<div className="empty">Đang tải...</div>}{error&&<div className="alert alert-error">{error}</div>}<div className="booking-list">{items.map(booking=><article className="booking-card" key={booking.id}><Link to={`/bookings/${booking.id}`} className="booking-main"><small>{booking.bookingCode}</small><h3>{booking.showtime.movieTitle}</h3><p>{booking.showtime.cinemaName} · {dateTime(booking.showtime.startTime)}</p><p>Ghế {booking.seats.map(s=>s.code).join(', ')}</p></Link><div className="booking-side"><StatusBadge value={booking.status}/><strong>{money(booking.totalAmount)}</strong>{booking.status==='PENDING'&&<div className="booking-actions"><button className="btn btn-sm" onClick={()=>resume(booking)}>Tiếp tục thanh toán</button><button className="btn btn-secondary btn-sm" disabled={workingId===booking.id} onClick={()=>void cancel(booking)}>Hủy & trả ghế</button></div>}</div></article>)}</div>{!loading&&!items.length&&<div className="empty"><p>Bạn chưa có booking nào.</p><Link className="btn" to="/movies">Xem phim đang chiếu</Link></div>}</div>;
}
