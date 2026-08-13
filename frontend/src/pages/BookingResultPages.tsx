import { Link, useSearchParams } from 'react-router-dom';

export function BookingConfirmedPage() {
  const [params] = useSearchParams();
  const bookingId = params.get('id');

  return (
    <div className="page-center">
      <div className="result-card success">
        <div className="result-icon">✓</div>
        <h1>Đặt vé thành công</h1>
        <p>Thanh toán đã được xác nhận và vé đã sẵn sàng.</p>
        {bookingId && (
          <Link className="btn" to={`/bookings/${bookingId}`}>
            Xem chi tiết vé
          </Link>
        )}
        <Link className="btn btn-secondary" to="/bookings">
          Vé của tôi
        </Link>
      </div>
    </div>
  );
}

export function PaymentFailedPage() {
  return (
    <div className="page-center">
      <div className="result-card fail">
        <div className="result-icon">!</div>
        <h1>Thanh toán thất bại</h1>
        <p>Không có khoản tiền thật nào được xử lý. Ghế đã được giải phóng.</p>
        <Link className="btn" to="/movies">
          Chọn lại phim
        </Link>
        <Link className="btn btn-secondary" to="/bookings">
          Vé của tôi
        </Link>
      </div>
    </div>
  );
}
