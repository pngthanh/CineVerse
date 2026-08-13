import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { StatusBadge } from '../../components/StatusBadge';
import { api } from '../../lib/api';
import { dateTime, money } from '../../lib/format';
import type { Booking, BookingStatus, PaymentStatus } from '../../types';

export function AdminBookingsPage() {
    const [items, setItems] = useState<Booking[]>([]);
    const [query, setQuery] = useState('');
    const [bookingStatus, setBookingStatus] = useState<BookingStatus | ''>('');
    const [paymentStatus, setPaymentStatus] = useState<PaymentStatus | ''>('');

    useEffect(() => {
        void api<Booking[]>('/admin/bookings').then(setItems);
    }, []);

    const filtered = useMemo(() => {
        const keyword = query.trim().toLowerCase();
        return items.filter((booking) => {
            const matchesQuery = !keyword
                || booking.bookingCode.toLowerCase().includes(keyword)
                || booking.showtime.movieTitle.toLowerCase().includes(keyword)
                || booking.showtime.cinemaName.toLowerCase().includes(keyword);
            const matchesBooking = !bookingStatus || booking.status === bookingStatus;
            const matchesPayment = !paymentStatus || booking.paymentStatus === paymentStatus;
            return matchesQuery && matchesBooking && matchesPayment;
        });
    }, [items, query, bookingStatus, paymentStatus]);

    return (
        <div className="admin-page">
            <div className="page-title">
                <h1>Đặt vé</h1>
                <p>Tra cứu, lọc và mở chi tiết booking khi cần xử lý vấn đề phát sinh.</p>
            </div>

            <div className="admin-booking-filters panel">
                <input
                    type="search"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Tìm mã booking, phim hoặc rạp..."
                />
                <select value={bookingStatus} onChange={(event) => setBookingStatus(event.target.value as BookingStatus | '')}>
                    <option value="">Tất cả trạng thái đặt vé</option>
                    <option value="PENDING">Đang chờ</option>
                    <option value="CONFIRMED">Đã xác nhận</option>
                    <option value="CANCELLED">Đã hủy</option>
                    <option value="COMPLETED">Hoàn tất</option>
                </select>
                <select value={paymentStatus} onChange={(event) => setPaymentStatus(event.target.value as PaymentStatus | '')}>
                    <option value="">Tất cả thanh toán</option>
                    <option value="PENDING">Đang chờ</option>
                    <option value="SUCCESS">Thành công</option>
                    <option value="FAILED">Thất bại</option>
                </select>
                <span>{filtered.length}/{items.length} booking</span>
            </div>

            <section className="panel table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Mã</th><th>Phim</th><th>Suất</th><th>Tiền</th><th>Đặt vé</th><th>Thanh toán</th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {filtered.map((booking) => (
                            <tr key={booking.id}>
                                <td>{booking.bookingCode}</td>
                                <td>{booking.showtime.movieTitle}</td>
                                <td>{dateTime(booking.showtime.startTime)}</td>
                                <td>{money(booking.totalAmount)}</td>
                                <td><StatusBadge value={booking.status} /></td>
                                <td><StatusBadge value={booking.paymentStatus} /></td>
                                <td><Link className="btn btn-sm btn-secondary" to={`/admin/bookings/${booking.id}`}>Chi tiết</Link></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {filtered.length === 0 && <div className="empty">Không tìm thấy booking phù hợp.</div>}
            </section>
        </div>
    );
}
