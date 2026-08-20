import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookingSteps } from '../components/BookingSteps';
import { useCountdown } from '../hooks/useCountdown';
import { ApiError, api } from '../lib/api';
import { money } from '../lib/format';
import type { Booking } from '../types';

interface VnPayCreateResponse {
  bookingId: number;
  transactionReference: string;
  paymentUrl: string;
}

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

  const payWithVnPay = async () => {
    if (!booking || countdown.expired) return;
    setLoading(true);
    setError('');
    try {
      const response = await api<VnPayCreateResponse>('/payments/vnpay/create', {
        method: 'POST',
        body: JSON.stringify({ bookingId: booking.id }),
      });
      window.location.assign(response.paymentUrl);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : 'Không thể khởi tạo giao dịch VNPAY.',
      );
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
        <span className="eyebrow">VNPAY SANDBOX</span>
        <h1>Thanh toán qua VNPAY</h1>
        <p>
          Giao dịch được chuyển sang Cổng thanh toán VNPAY Sandbox để kiểm thử luồng thanh toán thực
          tế.
        </p>
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

      <div className="two-col payment-vnpay-layout">
        <section className="panel vnpay-payment-card">
          <span className="eyebrow">PHƯƠNG THỨC THANH TOÁN</span>
          <h2>VNPAY</h2>
          <p>
            Bạn sẽ được chuyển sang trang VNPAY Sandbox để chọn ngân hàng hoặc hình thức thanh toán.
            CineVerse chỉ xác nhận vé sau khi backend kiểm tra chữ ký và trạng thái giao dịch trả về
            từ VNPAY.
          </p>

          <button
            disabled={loading || countdown.expired}
            className="btn btn-block vnpay-pay-button"
            onClick={() => void payWithVnPay()}
          >
            {loading
              ? 'Đang chuyển sang VNPAY...'
              : `Thanh toán ${money(booking.totalAmount)} qua VNPAY`}
          </button>
          <button
            className="btn btn-secondary btn-block"
            onClick={() => navigate('/checkout')}
            disabled={loading}
          >
            Quay lại kiểm tra đơn hàng
          </button>
        </section>

        <aside className="panel sticky">
          <h3>Đơn hàng</h3>
          <p>
            <strong>{booking.showtime.movieTitle}</strong>
          </p>
          <p className="muted">
            {booking.showtime.cinemaName} · {booking.showtime.roomName}
          </p>
          <p>Ghế {booking.seats.map((seat) => seat.code).join(', ')}</p>
          <div className="summary-row">
            <span>Tiền ghế</span>
            <strong>{money(booking.seatAmount)}</strong>
          </div>
          <div className="summary-row">
            <span>Bắp nước</span>
            <strong>{money(booking.concessionAmount)}</strong>
          </div>
          {booking.discountAmount > 0 && (
            <div className="summary-row discount-row">
              <span>Ưu đãi</span>
              <strong>-{money(booking.discountAmount)}</strong>
            </div>
          )}
          <div className="summary-row total">
            <span>Tổng</span>
            <strong>{money(booking.totalAmount)}</strong>
          </div>
        </aside>
      </div>
    </div>
  );
}
