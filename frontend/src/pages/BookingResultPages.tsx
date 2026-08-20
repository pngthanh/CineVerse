import { Link, useSearchParams } from 'react-router-dom';

export function BookingConfirmedPage() {
    const [params] = useSearchParams();
    const bookingId = params.get('id');

    if (bookingId) {
        sessionStorage.removeItem('cineverse_booking');
        sessionStorage.removeItem('cineverse_hold');
    }

    return (
        <div className="page-center">
            <div className="result-card success">
                <div className="result-icon">✓</div>
                <h1>Thanh toán VNPAY thành công</h1>
                <p>Giao dịch đã được xác minh và vé CineVerse đã được phát hành.</p>
                {bookingId && <Link className="btn" to={`/bookings/${bookingId}`}>Xem chi tiết vé</Link>}
                <Link className="btn btn-secondary" to="/bookings">Vé của tôi</Link>
            </div>
        </div>
    );
}

export function PaymentFailedPage() {
    const [params] = useSearchParams();
    sessionStorage.removeItem('cineverse_booking');
    sessionStorage.removeItem('cineverse_hold');
    const bookingId = params.get('id');
    const code = params.get('code');

    return (
        <div className="page-center">
            <div className="result-card fail">
                <div className="result-icon">!</div>
                <h1>Thanh toán VNPAY chưa thành công</h1>
                <p>
                    Giao dịch chưa được xác nhận{code ? ` (mã phản hồi ${code})` : ''}.
                    Booking đã được hủy và ghế được giải phóng để tránh giữ chỗ không thanh toán.
                </p>
                {bookingId && <Link className="btn btn-secondary" to={`/bookings/${bookingId}`}>Xem booking</Link>}
                <Link className="btn" to="/movies">Chọn lại phim</Link>
            </div>
        </div>
    );
}
