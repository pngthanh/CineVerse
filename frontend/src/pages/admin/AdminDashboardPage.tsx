import { useEffect, useMemo, useState } from 'react';
import { api } from '../../lib/api';
import { money } from '../../lib/format';

interface CinemaRevenue {
    cinemaId: number;
    cinemaName: string;
    bookings: number;
    ticketsSold: number;
    seatRevenue: number;
    concessionRevenue: number;
    discountAmount: number;
    netRevenue: number;
    movies: MovieRevenue[];
    concessions: ConcessionRevenue[];
}

interface MovieRevenue {
    movieId: number;
    movieTitle: string;
    posterUrl?: string;
    bookings: number;
    ticketsSold: number;
    ticketRevenue: number;
}

interface ConcessionRevenue {
    itemId: number;
    itemName: string;
    quantity: number;
    revenue: number;
}

interface Stats {
    totalUsers: number;
    totalBookings: number;
    confirmedBookings: number;
    totalMovies: number;
    totalCinemas: number;
    ticketsSold: number;
    grossSeatRevenue: number;
    concessionRevenue: number;
    discountAmount: number;
    netRevenue: number;
    cinemas: CinemaRevenue[];
    movies: MovieRevenue[];
    concessions: ConcessionRevenue[];
}

export function AdminDashboardPage() {
    const [stats, setStats] = useState<Stats | null>(null);
    const [selectedCinemaId, setSelectedCinemaId] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        void api<Stats>('/admin/dashboard')
            .then(setStats)
            .catch(() => setError('Không thể tải dữ liệu thống kê.'));
    }, []);

    const selectedCinema = useMemo(
        () => stats?.cinemas.find((cinema) => cinema.cinemaId === Number(selectedCinemaId)),
        [stats, selectedCinemaId],
    );
    const displayedMovies = selectedCinema?.movies ?? stats?.movies ?? [];
    const displayedConcessions = selectedCinema?.concessions ?? stats?.concessions ?? [];

    const maxMovieTickets = Math.max(1, ...displayedMovies.map((movie) => movie.ticketsSold), 1);
    const maxCinemaRevenue = Math.max(1, ...(stats?.cinemas.map((cinema) => cinema.netRevenue) ?? [1]));

    if (error) return <div className="alert alert-error">{error}</div>;

    return (
        <div className="admin-page dashboard-page">
            <div className="page-title dashboard-title-row">
                <div>
                    <h1>Tổng quan vận hành</h1>
                    <p>Doanh thu, vé bán, phim và bắp nước từ các booking đã xác nhận.</p>
                </div>
                <select value={selectedCinemaId} onChange={(event) => setSelectedCinemaId(event.target.value)}>
                    <option value="">Toàn hệ thống</option>
                    {stats?.cinemas.map((cinema) => (
                        <option key={cinema.cinemaId} value={cinema.cinemaId}>{cinema.cinemaName}</option>
                    ))}
                </select>
            </div>

            <div className="dashboard-kpi-grid">
                <div className="dashboard-kpi primary">
                    <span>Doanh thu thực nhận</span>
                    <strong>{money(selectedCinema?.netRevenue ?? stats?.netRevenue ?? 0)}</strong>
                    <small>Sau voucher/giảm giá</small>
                </div>
                <div className="dashboard-kpi">
                    <span>Doanh thu vé gốc</span>
                    <strong>{money(selectedCinema?.seatRevenue ?? stats?.grossSeatRevenue ?? 0)}</strong>
                    <small>Chưa trừ voucher</small>
                </div>
                <div className="dashboard-kpi">
                    <span>Bắp nước</span>
                    <strong>{money(selectedCinema?.concessionRevenue ?? stats?.concessionRevenue ?? 0)}</strong>
                    <small>Doanh thu concession</small>
                </div>
                <div className="dashboard-kpi">
                    <span>Vé đã bán</span>
                    <strong>{selectedCinema?.ticketsSold ?? stats?.ticketsSold ?? 0}</strong>
                    <small>{selectedCinema?.bookings ?? stats?.confirmedBookings ?? 0} booking xác nhận</small>
                </div>
            </div>

            <div className="dashboard-mini-stats">
                <span><strong>{stats?.totalUsers ?? 0}</strong> người dùng</span>
                <span><strong>{stats?.totalBookings ?? 0}</strong> booking tổng</span>
                <span><strong>{stats?.totalMovies ?? 0}</strong> phim</span>
                <span><strong>{stats?.totalCinemas ?? 0}</strong> rạp</span>
                <span><strong>{money(stats?.discountAmount ?? 0)}</strong> ưu đãi đã dùng</span>
            </div>

            <div className="dashboard-grid-two">
                <section className="panel dashboard-panel">
                    <div className="section-head compact">
                        <div>
                            <h2>Doanh thu theo rạp</h2>
                            <p>So sánh doanh thu thực nhận của từng cụm rạp.</p>
                        </div>
                    </div>
                    <div className="dashboard-ranking-list">
                        {stats?.cinemas.map((cinema) => (
                            <button
                                className={`dashboard-ranking-row ${selectedCinemaId === String(cinema.cinemaId) ? 'active' : ''}`}
                                key={cinema.cinemaId}
                                type="button"
                                onClick={() => setSelectedCinemaId(String(cinema.cinemaId))}
                            >
                                <div className="dashboard-ranking-label">
                                    <strong>{cinema.cinemaName}</strong>
                                    <span>{cinema.ticketsSold} vé · {cinema.bookings} booking</span>
                                </div>
                                <div className="dashboard-bar-track">
                                    <span style={{ width: `${Math.max(4, cinema.netRevenue / maxCinemaRevenue * 100)}%` }} />
                                </div>
                                <strong>{money(cinema.netRevenue)}</strong>
                            </button>
                        ))}
                        {stats?.cinemas.length === 0 && <div className="empty">Chưa có doanh thu xác nhận.</div>}
                    </div>
                </section>

                <section className="panel dashboard-panel">
                    <div className="section-head compact">
                        <div>
                            <h2>Chi tiết rạp</h2>
                            <p>{selectedCinema?.cinemaName ?? 'Chọn một rạp để xem riêng'}</p>
                        </div>
                    </div>
                    {selectedCinema ? (
                        <div className="dashboard-detail-grid">
                            <div><span>Doanh thu vé</span><strong>{money(selectedCinema.seatRevenue)}</strong></div>
                            <div><span>Bắp nước</span><strong>{money(selectedCinema.concessionRevenue)}</strong></div>
                            <div><span>Giảm giá</span><strong>-{money(selectedCinema.discountAmount)}</strong></div>
                            <div className="highlight"><span>Thực nhận</span><strong>{money(selectedCinema.netRevenue)}</strong></div>
                        </div>
                    ) : (
                        <div className="dashboard-system-note">
                            <strong>Toàn hệ thống</strong>
                            <p>Chọn một rạp ở bảng bên trái hoặc dropdown phía trên để drill-down số liệu riêng.</p>
                        </div>
                    )}
                </section>
            </div>

            <div className="dashboard-grid-two">
                <section className="panel dashboard-panel">
                    <div className="section-head compact">
                        <div>
                            <h2>Phim bán chạy</h2>
                            <p>{selectedCinema ? `Riêng ${selectedCinema.cinemaName}` : 'Xếp theo số ghế/vé đã thanh toán thành công.'}</p>
                        </div>
                    </div>
                    <div className="dashboard-movie-list">
                        {displayedMovies.slice(0, 8).map((movie, index) => (
                            <article className="dashboard-movie-row" key={movie.movieId}>
                                <span className="dashboard-rank">{String(index + 1).padStart(2, '0')}</span>
                                <div
                                    className="dashboard-movie-poster"
                                    style={movie.posterUrl ? { backgroundImage: `url(${movie.posterUrl})` } : undefined}
                                >
                                    {!movie.posterUrl && movie.movieTitle.slice(0, 1)}
                                </div>
                                <div className="dashboard-movie-info">
                                    <strong>{movie.movieTitle}</strong>
                                    <span>{movie.ticketsSold} vé · {movie.bookings} booking</span>
                                    <div className="dashboard-bar-track movie">
                                        <span style={{ width: `${Math.max(4, movie.ticketsSold / maxMovieTickets * 100)}%` }} />
                                    </div>
                                </div>
                                <strong>{money(movie.ticketRevenue)}</strong>
                            </article>
                        ))}
                        {displayedMovies.length === 0 && <div className="empty">Chưa có dữ liệu phim.</div>}
                    </div>
                </section>

                <section className="panel dashboard-panel">
                    <div className="section-head compact">
                        <div>
                            <h2>Bắp nước & combo</h2>
                            <p>{selectedCinema ? `Riêng ${selectedCinema.cinemaName}` : 'Số lượng bán và doanh thu theo từng sản phẩm.'}</p>
                        </div>
                    </div>
                    <div className="dashboard-concession-list">
                        {displayedConcessions.map((item) => (
                            <div className="dashboard-concession-row" key={item.itemId}>
                                <div>
                                    <strong>{item.itemName}</strong>
                                    <span>{item.quantity} sản phẩm đã bán</span>
                                </div>
                                <strong>{money(item.revenue)}</strong>
                            </div>
                        ))}
                        {displayedConcessions.length === 0 && <div className="empty">Chưa có dữ liệu bắp nước.</div>}
                    </div>
                </section>
            </div>
        </div>
    );
}
