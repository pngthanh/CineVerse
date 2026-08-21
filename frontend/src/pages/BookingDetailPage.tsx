import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { StatusBadge } from '../components/StatusBadge';
import { TicketQrCard } from '../components/TicketQrCard';
import { api } from '../lib/api';
import { dateTime, money, seatTypeLabel } from '../lib/format';
import type { Booking } from '../types';

export function BookingDetailPage() {
    const { id } = useParams();
    const [booking, setBooking] = useState<Booking | null>(null);
    const [loading, setLoading] = useState(Boolean(id));
    const [error, setError] = useState(id ? '' : 'Booking không hợp lệ.');

    useEffect(() => {
        if (!id) return;

        void api<Booking>(`/bookings/${id}`)
            .then(setBooking)
            .catch(() => setError('Không thể tải chi tiết booking.'))
            .finally(() => setLoading(false));
    }, [id]);

    if (loading) {
        return <div className="page-center">Đang tải booking...</div>;
    }

    if (error || !booking) {
        return (
            <div className="page-center">
                <div className="error-card">
                    <h2>Không thể mở booking</h2>
                    <p>{error || 'Không tìm thấy booking.'}</p>
                    <Link className="btn" to="/bookings">
                        Quay lại Vé của tôi
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="container page">
            <div className="page-title">
                <p>Vé của tôi / {booking.bookingCode}</p>
                <h1>Chi tiết đặt vé</h1>
                <StatusBadge value={booking.status} />
            </div>

            {booking.cancellationReason && <div className="alert alert-error">{booking.status === 'REFUND_PENDING' ? 'Booking đang chờ hoàn tiền: ' : 'Booking đã bị hủy: '}{booking.cancellationReason}</div>}

            <div className="two-col">
                <section className="panel">
                    <h2>{booking.showtime.movieTitle}</h2>
                    <p>
                        {booking.showtime.cinemaName} · {booking.showtime.roomName}
                    </p>
                    <p>{dateTime(booking.showtime.startTime)}</p>

                    <hr />
                    <h3>Vé điện tử</h3>
                    <div className="ticket-code">{booking.ticketCode ?? 'Chưa phát hành'}</div>
                    {booking.ticketCode && booking.ticketStatus !== 'CANCELLED' && <TicketQrCard ticketCode={booking.ticketCode} />}

                    {booking.concessions.length > 0 && (
                        <>
                            <hr />
                            <h3>Bắp nước</h3>
                            <div className="booking-concession-list">
                                {booking.concessions.map((item) => (
                                    <div className="summary-row" key={item.itemId}>
                                        <span>{item.name} × {item.quantity}</span>
                                        <strong>{money(item.totalPrice)}</strong>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}

                    <div className="details-list">
                        <div>
                            <dt>Mã booking</dt>
                            <dd>{booking.bookingCode}</dd>
                        </div>
                        <div>
                            <dt>Ghế</dt>
                            <dd>{booking.seats.map((seat) => seat.code).join(', ')}</dd>
                        </div>
                        <div>
                            <dt>Loại ghế</dt>
                            <dd>
                                {[...new Set(booking.seats.map((seat) => seatTypeLabel(seat.type)))].join(', ')}
                            </dd>
                        </div>
                        <div>
                            <dt>Trạng thái vé</dt>
                            <dd><StatusBadge value={booking.ticketStatus} /></dd>
                        </div>
                        {booking.ticketCheckedInAt && <div><dt>Check-in lúc</dt><dd>{dateTime(booking.ticketCheckedInAt)}</dd></div>}
                        {booking.ticketCheckedInByName && <div><dt>Nhân viên check-in</dt><dd>{booking.ticketCheckedInByName}</dd></div>}
                    </div>
                </section>

                <aside className="panel sticky">
                    <h3>Thanh toán</h3>
                    <div className="summary-row">
                        <span>Trạng thái</span>
                        <StatusBadge value={booking.paymentStatus} />
                    </div>
                    <div className="summary-row"><span>Phương thức</span><strong>{booking.paymentProvider ?? 'Chưa có'}</strong></div>
                    {booking.paymentBankCode && <div className="summary-row"><span>Ngân hàng</span><strong>{booking.paymentBankCode}</strong></div>}
                    {booking.paymentTransactionNo && <div className="summary-row"><span>Mã giao dịch</span><strong className="mono-value">{booking.paymentTransactionNo}</strong></div>}
                    <div className="summary-row">
                        <span>Vé xem phim</span>
                        <strong>{money(booking.seatAmount)}</strong>
                    </div>
                    <div className="summary-row">
                        <span>Bắp nước</span>
                        <strong>{money(booking.concessionAmount)}</strong>
                    </div>
                    <div className="summary-row">
                        <span>Giá gốc</span>
                        <strong>{money(booking.subtotalAmount)}</strong>
                    </div>
                    <div className="summary-row">
                        <span>Voucher</span>
                        <strong>{booking.voucherCode ?? 'Không sử dụng'}</strong>
                    </div>
                    {booking.discountAmount > 0 && (
                        <div className="summary-row discount-row">
                            <span>Được giảm</span>
                            <strong>-{money(booking.discountAmount)}</strong>
                        </div>
                    )}
                    <div className="summary-row total">
                        <span>Thành tiền</span>
                        <strong>{money(booking.totalAmount)}</strong>
                    </div>
                    <Link className="btn btn-secondary btn-block" to="/bookings">
                        Quay lại Vé của tôi
                    </Link>
                </aside>
            </div>
        </div>
    );
}
