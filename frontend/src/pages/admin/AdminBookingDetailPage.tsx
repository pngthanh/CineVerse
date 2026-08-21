import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { StatusBadge } from '../../components/StatusBadge';
import { api } from '../../lib/api';
import { dateTime, money, seatTypeLabel } from '../../lib/format';
import type { Booking } from '../../types';

export function AdminBookingDetailPage() {
    const { id } = useParams();
    const [booking, setBooking] = useState<Booking | null>(null);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!id) return;
        void api<Booking>(`/admin/bookings/${id}`)
            .then(setBooking)
            .catch(() => setError('Không thể tải chi tiết booking.'));
    }, [id]);

    if (error) return <div className="admin-page"><div className="alert alert-error">{error}</div></div>;
    if (!booking) return <div className="admin-page"><div className="page-center">Đang tải booking...</div></div>;

    return (
        <div className="admin-page">
            <div className="admin-detail-back"><Link to="/admin/bookings">← Quay lại danh sách đặt vé</Link></div>
            <div className="page-title admin-detail-title">
                <div>
                    <h1>Chi tiết booking</h1>
                    <p>{booking.bookingCode}</p>
                </div>
                <StatusBadge value={booking.status} />
            </div>

            {booking.cancellationReason && <div className="alert alert-error">{booking.status === 'REFUND_PENDING' ? 'Chờ hoàn tiền: ' : 'Lý do hủy: '}{booking.cancellationReason}</div>}

            <div className="admin-detail-grid">
                <section className="panel">
                    <div className="admin-detail-section admin-customer-info">
                        <h3>Tài khoản người đặt</h3>
                        {booking.user ? (
                            <div className="details-list">
                                <div><dt>Họ tên</dt><dd>{booking.user.fullName}</dd></div>
                                <div><dt>Email</dt><dd>{booking.user.email}</dd></div>
                                <div><dt>ID tài khoản</dt><dd>#{booking.user.id}</dd></div>
                                <div><dt>Vai trò</dt><dd>{booking.user.role}</dd></div>
                                <div><dt>Trạng thái</dt><dd><StatusBadge value={booking.user.status} /></dd></div>
                                <div><dt>Ngày tạo tài khoản</dt><dd>{dateTime(booking.user.createdAt)}</dd></div>
                            </div>
                        ) : (
                            <div className="alert alert-warning">
                                Chưa nhận được thông tin tài khoản từ backend. Hãy restart backend để tải dữ liệu mới.
                            </div>
                        )}
                    </div>

                    <div className="admin-detail-section">
                        <h2>{booking.showtime.movieTitle}</h2>
                    <div className="details-list">
                        <div><dt>Rạp</dt><dd>{booking.showtime.cinemaName}</dd></div>
                        <div><dt>Phòng</dt><dd>{booking.showtime.roomName}</dd></div>
                        <div><dt>Suất chiếu</dt><dd>{dateTime(booking.showtime.startTime)}</dd></div>
                        <div><dt>Ghế</dt><dd>{booking.seats.map((seat) => `${seat.code} · ${seatTypeLabel(seat.type)}`).join(', ')}</dd></div>
                        <div><dt>Tạo lúc</dt><dd>{dateTime(booking.createdAt)}</dd></div>
                    </div>
                    </div>

                    {booking.concessions.length > 0 && (
                        <div className="admin-detail-section">
                            <h3>Bắp nước</h3>
                            {booking.concessions.map((item) => (
                                <div className="summary-row" key={item.itemId}>
                                    <span>{item.name} × {item.quantity}</span>
                                    <strong>{money(item.totalPrice)}</strong>
                                </div>
                            ))}
                        </div>
                    )}
                </section>

                <aside className="panel sticky">
                    <h3>Thanh toán</h3>
                    <div className="summary-row"><span>Trạng thái</span><StatusBadge value={booking.paymentStatus} /></div>
                    <div className="summary-row"><span>Cổng thanh toán</span><strong>{booking.paymentProvider ?? 'Chưa khởi tạo'}</strong></div>
                    <div className="summary-row"><span>Phương thức</span><strong>{booking.paymentMethod ?? '—'}</strong></div>
                    <div className="summary-row"><span>Ngân hàng</span><strong>{booking.paymentBankCode ?? '—'}</strong></div>
                    <div className="summary-row"><span>Loại thẻ</span><strong>{booking.paymentCardType ?? '—'}</strong></div>
                    <div className="summary-row"><span>Mã tham chiếu</span><strong className="mono-value">{booking.paymentTransactionReference ?? '—'}</strong></div>
                    <div className="summary-row"><span>Mã giao dịch VNPAY</span><strong className="mono-value">{booking.paymentTransactionNo ?? '—'}</strong></div>
                    <div className="summary-row"><span>Mã phản hồi</span><strong>{booking.paymentResponseCode ?? '—'}</strong></div>
                    {booking.paymentPaidAt && <div className="summary-row"><span>Thanh toán lúc</span><strong>{dateTime(booking.paymentPaidAt)}</strong></div>}
                    <div className="summary-row"><span>Tiền ghế</span><strong>{money(booking.seatAmount)}</strong></div>
                    <div className="summary-row"><span>Bắp nước</span><strong>{money(booking.concessionAmount)}</strong></div>
                    <div className="summary-row"><span>Giá gốc</span><strong>{money(booking.subtotalAmount)}</strong></div>
                    <div className="summary-row"><span>Voucher</span><strong>{booking.voucherCode ?? 'Không sử dụng'}</strong></div>
                    {booking.discountAmount > 0 && <div className="summary-row discount-row"><span>Được giảm</span><strong>-{money(booking.discountAmount)}</strong></div>}
                    <div className="summary-row total"><span>Thành tiền</span><strong>{money(booking.totalAmount)}</strong></div>
                    <div className="summary-row"><span>Mã vé</span><strong>{booking.ticketCode ?? 'Chưa phát hành'}</strong></div>
                    <div className="summary-row"><span>Trạng thái vé</span><StatusBadge value={booking.ticketStatus} /></div>
                    {booking.ticketCheckedInAt && <div className="summary-row"><span>Check-in lúc</span><strong>{dateTime(booking.ticketCheckedInAt)}</strong></div>}
                    {booking.ticketCheckedInByName && <div className="summary-row"><span>Nhân viên check-in</span><strong>{booking.ticketCheckedInByName}</strong></div>}
                </aside>
            </div>
        </div>
    );
}
