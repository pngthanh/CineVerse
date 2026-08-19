import { Link } from 'react-router-dom';
export function Footer() {
    return (
        <footer className="footer">
            <div className="container footer-top">
                <div><div className="brand brand-lockup"><span>CineVerse</span></div><p>Trải nghiệm đặt vé điện ảnh hiện đại, trực quan và an toàn.</p></div>
                <div className="footer-cta"><strong>Đặt vé cho buổi xem tiếp theo</strong><Link className="btn btn-sm" to="/movies">Khám phá phim</Link></div>
            </div>
            <div className="container footer-grid">
                <div><strong>Khám phá</strong><Link to="/movies">Phim đang chiếu</Link><Link to="/movies">Phim sắp chiếu</Link><Link to="/cinemas">Hệ thống rạp</Link></div>
                <div><strong>Đặt vé</strong><Link to="/bookings">Vé của tôi</Link><Link to="/account">Tài khoản</Link><span>Giữ ghế 5 phút</span></div>
                <div><strong>CineVerse</strong><span>REST API · Spring Boot</span><span>React · PostgreSQL</span><span>Demo portfolio 2026</span></div>
                <div><strong>Hỗ trợ</strong><span>Thanh toán mô phỏng</span><span>Swagger/OpenAPI</span><span>CI/CD & Docker ready</span></div>
            </div>
            <div className="container footer-bottom">© 2026 CineVerse · Software Engineering Portfolio Project</div>
        </footer>
    );
}
