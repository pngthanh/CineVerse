import { useEffect, useMemo, useState } from 'react';
import { api } from '../../lib/api';
import { money } from '../../lib/format';

interface TrendPoint { label: string; revenue: number; tickets: number }
interface CinemaRow { id: number; name: string; bookings: number; tickets: number; revenue: number }
interface MovieRow { id: number; title: string; posterUrl?: string; bookings: number; tickets: number; revenue: number }
interface ConcessionRow { id: number; name: string; quantity: number; revenue: number }
interface VoucherRow { code: string; uses: number; discountAmount: number }
interface PaymentMethodRow { method: string; transactions: number; revenue: number }
interface Stats {
    bookings: number;
    ticketsSold: number;
    checkedInTickets: number;
    seatRevenue: number;
    concessionRevenue: number;
    discountAmount: number;
    netRevenue: number;
    trend: TrendPoint[];
    cinemas: CinemaRow[];
    movies: MovieRow[];
    concessions: ConcessionRow[];
    vouchers: VoucherRow[];
    paymentMethods: PaymentMethodRow[];
}
interface Option { id: number; name?: string; title?: string }

function isoDate(date: Date) { return date.toISOString().slice(0, 10); }

export function AdminDashboardPage() {
    const today = useMemo(() => new Date(), []);
    const initialFrom = useMemo(() => {
        const date = new Date(today);
        date.setDate(date.getDate() - 29);
        return isoDate(date);
    }, [today]);
    const [from, setFrom] = useState(initialFrom);
    const [to, setTo] = useState(isoDate(today));
    const [cinemaId, setCinemaId] = useState('');
    const [movieId, setMovieId] = useState('');
    const [stats, setStats] = useState<Stats | null>(null);
    const [cinemas, setCinemas] = useState<Option[]>([]);
    const [movies, setMovies] = useState<Option[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        void Promise.all([
            api<Option[]>('/admin/cinemas'),
            api<Option[]>('/admin/movies'),
        ]).then(([cinemaRows, movieRows]) => {
            setCinemas(cinemaRows);
            setMovies(movieRows);
        });
    }, []);

    useEffect(() => {
        const params = new URLSearchParams();
        if (from) params.set('from', from);
        if (to) params.set('to', to);
        if (cinemaId) params.set('cinemaId', cinemaId);
        if (movieId) params.set('movieId', movieId);
        void api<Stats>(`/admin/dashboard/analytics?${params.toString()}`)
            .then(setStats)
            .catch(() => setError('Không thể tải dữ liệu thống kê.'))
            .finally(() => setLoading(false));
    }, [from, to, cinemaId, movieId]);

    const beginFilterChange = () => {
        setLoading(true);
        setError('');
    };

    const maxTrend = Math.max(1, ...(stats?.trend.map((row) => row.revenue) ?? [1]));
    const maxCinema = Math.max(1, ...(stats?.cinemas.map((row) => row.revenue) ?? [1]));
    const maxMovie = Math.max(1, ...(stats?.movies.map((row) => row.tickets) ?? [1]));
    const checkInRate = stats?.ticketsSold ? Math.round(stats.checkedInTickets / stats.ticketsSold * 100) : 0;

    return <div className="admin-page g11-dashboard">
        <div className="page-title g11-title">
            <div><h1>Dashboard & thống kê</h1><p>Theo dõi doanh thu và vận hành từ các booking đã xác nhận.</p></div>
        </div>

        <section className="panel g11-filters">
            <label>Từ ngày<input type="date" value={from} onChange={(e) => { beginFilterChange(); setFrom(e.target.value); }} /></label>
            <label>Đến ngày<input type="date" value={to} onChange={(e) => { beginFilterChange(); setTo(e.target.value); }} /></label>
            <label>Rạp<select value={cinemaId} onChange={(e) => { beginFilterChange(); setCinemaId(e.target.value); }}><option value="">Tất cả rạp</option>{cinemas.map((row) => <option key={row.id} value={row.id}>{row.name}</option>)}</select></label>
            <label>Phim<select value={movieId} onChange={(e) => { beginFilterChange(); setMovieId(e.target.value); }}><option value="">Tất cả phim</option>{movies.map((row) => <option key={row.id} value={row.id}>{row.title}</option>)}</select></label>
            <button className="btn btn-secondary g11-reset-button" type="button" onClick={() => { beginFilterChange(); setFrom(initialFrom); setTo(isoDate(today)); setCinemaId(''); setMovieId(''); }}>Đặt lại</button>
        </section>

        {error && <div className="alert alert-error">{error}</div>}
        {loading && <div className="panel g11-loading">Đang tải thống kê...</div>}
        {!loading && stats && <>
            <div className="g11-kpis">
                <article><span>Doanh thu thực nhận</span><strong>{money(stats.netRevenue)}</strong><small>Sau voucher/giảm giá</small></article>
                <article><span>Vé đã bán</span><strong>{stats.ticketsSold}</strong><small>{stats.bookings} booking</small></article>
                <article><span>Check-in</span><strong>{stats.checkedInTickets}</strong><small>{checkInRate}% số vé đã bán</small></article>
                <article><span>Bắp nước</span><strong>{money(stats.concessionRevenue)}</strong><small>Doanh thu concession</small></article>
            </div>
            <div className="g11-summary-strip">
                <span>Vé gốc <b>{money(stats.seatRevenue)}</b></span>
                <span>Ưu đãi <b>-{money(stats.discountAmount)}</b></span>
                <span>Phương thức thanh toán <b>{stats.paymentMethods.length}</b></span>
                <span>Voucher đã dùng <b>{stats.vouchers.reduce((sum, row) => sum + row.uses, 0)}</b></span>
            </div>

            <div className="g11-grid-two">
                <section className="panel g11-panel"><h2>Doanh thu theo ngày / tháng</h2><p className="muted">Tự chuyển sang theo tháng khi khoảng lọc dài.</p><div className="g11-trend">{stats.trend.map((row) => <div className="g11-trend-col" key={row.label}><div className="g11-trend-value">{money(row.revenue)}</div><div className="g11-trend-bar"><span style={{ height: `${Math.max(5, row.revenue / maxTrend * 100)}%` }} /></div><small>{row.label}</small></div>)}{stats.trend.length === 0 && <div className="empty">Chưa có doanh thu trong khoảng này.</div>}</div></section>
                <section className="panel g11-panel"><h2>Phương thức thanh toán</h2><div className="g11-list">{stats.paymentMethods.map((row) => <div key={row.method}><span><b>{row.method}</b><small>{row.transactions} giao dịch</small></span><strong>{money(row.revenue)}</strong></div>)}{stats.paymentMethods.length === 0 && <div className="empty">Chưa có dữ liệu thanh toán.</div>}</div></section>
            </div>

            <div className="g11-grid-two">
                <section className="panel g11-panel"><h2>Doanh thu theo rạp</h2><div className="g11-ranking">{stats.cinemas.map((row) => <div key={row.id}><span><b>{row.name}</b><small>{row.tickets} vé · {row.bookings} booking</small></span><i><em style={{ width: `${Math.max(4, row.revenue / maxCinema * 100)}%` }} /></i><strong>{money(row.revenue)}</strong></div>)}{stats.cinemas.length === 0 && <div className="empty">Chưa có dữ liệu rạp.</div>}</div></section>
                <section className="panel g11-panel"><h2>Phim bán tốt</h2><div className="g11-ranking">{stats.movies.slice(0, 8).map((row) => <div key={row.id}><span><b>{row.title}</b><small>{row.tickets} vé · {row.bookings} booking</small></span><i><em style={{ width: `${Math.max(4, row.tickets / maxMovie * 100)}%` }} /></i><strong>{money(row.revenue)}</strong></div>)}{stats.movies.length === 0 && <div className="empty">Chưa có dữ liệu phim.</div>}</div></section>
            </div>

            <div className="g11-grid-two">
                <section className="panel g11-panel"><h2>Bắp nước & combo</h2><div className="g11-list">{stats.concessions.map((row) => <div key={row.id}><span><b>{row.name}</b><small>{row.quantity} sản phẩm</small></span><strong>{money(row.revenue)}</strong></div>)}{stats.concessions.length === 0 && <div className="empty">Chưa có dữ liệu concession.</div>}</div></section>
                <section className="panel g11-panel"><h2>Voucher</h2><div className="g11-list">{stats.vouchers.map((row) => <div key={row.code}><span><b>{row.code}</b><small>{row.uses} lượt sử dụng</small></span><strong>-{money(row.discountAmount)}</strong></div>)}{stats.vouchers.length === 0 && <div className="empty">Chưa có voucher được sử dụng.</div>}</div></section>
            </div>
        </>}
    </div>;
}
