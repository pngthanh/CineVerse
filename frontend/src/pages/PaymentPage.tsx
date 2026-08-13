import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookingSteps } from '../components/BookingSteps';
import { useCountdown } from '../hooks/useCountdown';
import { ApiError, api } from '../lib/api';
import { money } from '../lib/format';
import type { Booking } from '../types';
export function PaymentPage() {
  const navigate = useNavigate();
  const booking = useMemo(() => {
    try {
      return JSON.parse(sessionStorage.getItem('cineverse_booking') || 'null') as Booking | null;
    } catch {
      return null;
    }
  }, []);
  const countdown = useCountdown(booking?.expiresAt);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const pay = async (result: 'SUCCESS' | 'FAILED') => {
    if (!booking || countdown.expired) return;
    setLoading(true);
    setError('');
    try {
      await api('/payments/mock', {
        method: 'POST',
        body: JSON.stringify({ bookingId: booking.id, result }),
      });
      sessionStorage.removeItem('cineverse_booking');
      sessionStorage.removeItem('cineverse_hold');
      navigate(
        result === 'SUCCESS'
          ? `/booking-confirmed?id=${booking.id}`
          : `/payment-failed?id=${booking.id}`,
      );
    } catch (requestError) {
      setError(
        requestError instanceof ApiError ? requestError.message : 'Không thể xử lý thanh toán.',
      );
    } finally {
      setLoading(false);
    }
  };
  if (!booking) {
    return <div className="page-center">Không có booking chờ thanh toán.</div>;
  }
  return (
    <div className="container page">
      <BookingSteps active={4} />
      <div className="page-title">
        <span className="eyebrow">THANH TOÁN MÔ PHỎNG</span>
        <h1>Thanh toán mô phỏng</h1>
        <p>Trang này chỉ mô phỏng luồng thanh toán. Không xử lý tiền thật.</p>
      </div>

      <div className={`hold-timer ${countdown.expired ? 'expired' : ''}`} role="status">
        {countdown.expired ? (
          'Phiên giữ ghế đã hết hạn.'
        ) : (
          <>
            Ghế đang được giữ trong <strong>{countdown.label}</strong>
          </>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="two-col">
        <section className="panel">
          <h3>Cổng thanh toán CineVerse Demo</h3>
          <p>Chọn kết quả để kiểm thử luồng booking.</p>
          <button
            disabled={loading || countdown.expired}
            className="btn btn-block"
            onClick={() => pay('SUCCESS')}
          >
            {loading ? 'Đang xử lý...' : 'Mô phỏng thành công'}
          </button>
          <button
            disabled={loading || countdown.expired}
            className="btn btn-danger btn-block"
            onClick={() => pay('FAILED')}
          >
            Mô phỏng thất bại
          </button>
        </section>

        <aside className="panel">
          <h3>Đơn hàng</h3>
          <p>{booking.showtime.movieTitle}</p>
          <p className="muted">
            {booking.showtime.cinemaName} · {booking.showtime.roomName}
          </p>
          <p>Ghế {booking.seats.map((seat) => seat.code).join(', ')}</p>
          <div className="summary-row total">
            <span>Tổng</span>
            <strong>{money(booking.totalAmount)}</strong>
          </div>
        </aside>
      </div>
    </div>
  );
}
