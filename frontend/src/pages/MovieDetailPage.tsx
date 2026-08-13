import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { MovieCard } from '../components/MovieCard';
import { api } from '../lib/api';
import { timeOnly } from '../lib/format';
import type { Movie, Showtime } from '../types';

function youtubeEmbed(url?: string) {
    if (!url) return '';
    const match = url.match(/(?:v=|youtu\.be\/|embed\/)([A-Za-z0-9_-]{6,})/);
    return match ? `https://www.youtube.com/embed/${match[1]}?autoplay=1&rel=0` : '';
}

export function MovieDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [movie, setMovie] = useState<Movie | null>(null);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [showtimes, setShowtimes] = useState<Showtime[]>([]);
    const [trailerOpen, setTrailerOpen] = useState(false);
    const hotMoviesRef = useRef<HTMLDivElement | null>(null);
    const dragStartXRef = useRef(0);
    const dragStartScrollRef = useRef(0);
    const draggingRef = useRef(false);
    const draggedRef = useRef(false);
    const [draggingHotMovies, setDraggingHotMovies] = useState(false);

    useEffect(() => {
        if (!id) return;
        void api<Movie>(`/movies/${id}`).then(setMovie);
        void api<Movie[]>('/movies').then(setMovies).catch(() => setMovies([]));
        void api<Showtime[]>(`/showtimes?movieId=${id}`)
            .then(setShowtimes)
            .catch(() => setShowtimes([]));
    }, [id]);

    useEffect(() => {
        if (!trailerOpen) return;
        const closeOnEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') setTrailerOpen(false);
        };
        document.body.classList.add('modal-open');
        window.addEventListener('keydown', closeOnEscape);
        return () => {
            document.body.classList.remove('modal-open');
            window.removeEventListener('keydown', closeOnEscape);
        };
    }, [trailerOpen]);

    const embed = useMemo(() => youtubeEmbed(movie?.trailerUrl), [movie?.trailerUrl]);
    const hotMovies = useMemo(
        () => [...movies]
            .filter((item) => item.id !== movie?.id && item.status !== 'INACTIVE')
            .sort((a, b) =>
                (b.ticketsSold ?? 0) - (a.ticketsSold ?? 0)
                || (b.ratingAverage ?? 0) - (a.ratingAverage ?? 0)
                || (b.reviewCount ?? 0) - (a.reviewCount ?? 0))
            .slice(0, 6),
        [movie?.id, movies],
    );

    const scrollHotMovies = (direction: -1 | 1) => {
        const row = hotMoviesRef.current;
        if (!row) return;
        const distance = Math.max(460, Math.round(row.clientWidth * 0.78));
        row.scrollBy({ left: direction * distance, behavior: 'smooth' });
    };

    const startHotMoviesDrag = (event: React.PointerEvent<HTMLDivElement>) => {
        if (event.button !== 0) return;
        const row = hotMoviesRef.current;
        if (!row) return;
        draggingRef.current = true;
        draggedRef.current = false;
        dragStartXRef.current = event.clientX;
        dragStartScrollRef.current = row.scrollLeft;
        setDraggingHotMovies(true);
        row.setPointerCapture(event.pointerId);
    };

    const moveHotMoviesDrag = (event: React.PointerEvent<HTMLDivElement>) => {
        if (!draggingRef.current) return;
        const row = hotMoviesRef.current;
        if (!row) return;
        const delta = event.clientX - dragStartXRef.current;
        if (Math.abs(delta) > 5) draggedRef.current = true;
        row.scrollLeft = dragStartScrollRef.current - delta;
    };

    const stopHotMoviesDrag = (event: React.PointerEvent<HTMLDivElement>) => {
        if (!draggingRef.current) return;
        draggingRef.current = false;
        setDraggingHotMovies(false);
        const row = hotMoviesRef.current;
        if (row?.hasPointerCapture(event.pointerId)) row.releasePointerCapture(event.pointerId);
    };

    if (!movie) return <div className="page-center">Đang tải phim...</div>;

    return <>
        <section className="detail-hero">
            <div className="container">
                <button className="back-button" onClick={() => navigate(-1)}>← Trở về</button>
                <div className="detail-layout">
                    <div
                        className={`detail-poster poster ${movie.posterUrl ? 'poster-image' : 'poster-space'}`}
                        style={movie.posterUrl ? { backgroundImage: `url(${movie.posterUrl})` } : undefined}
                    />
                    <div>
                        <span className="eyebrow">{movie.status === 'NOW_SHOWING' ? 'ĐANG CHIẾU' : 'SẮP CHIẾU'}</span>
                        <h1>{movie.title}</h1>
                        <div className="detail-badges">
                            <span>{movie.ageRating}</span>
                            <span>{movie.durationMinutes} phút</span>
                            <span>★ {movie.ratingAverage?.toFixed(1)} / 10</span>
                            <span>{movie.reviewCount} đánh giá</span>
                        </div>
                        <p>{movie.description}</p>
                        <div className="row">
                            {movie.status === 'NOW_SHOWING' && <Link className="btn" to={`/showtimes/select?movieId=${movie.id}`}>Đặt vé</Link>}
                            <button className="btn btn-secondary" type="button" onClick={() => setTrailerOpen(true)} disabled={!embed}>▶ Xem trailer</button>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <div className="container page">
            <div className="detail-content-grid">
                <section>
                    <div className="content-card">
                        <span className="section-kicker">NỘI DUNG</span>
                        <h2>Giới thiệu phim</h2>
                        <p className="long-copy">{movie.description} CineVerse cập nhật lịch chiếu, thông tin phim và trạng thái ghế theo dữ liệu từ hệ thống.</p>
                        <dl className="details-list">
                            <div><dt>Đạo diễn</dt><dd>{movie.director || 'Đang cập nhật'}</dd></div>
                            <div><dt>Diễn viên</dt><dd>{movie.castNames || 'Đang cập nhật'}</dd></div>
                            <div><dt>Thể loại</dt><dd>{movie.genres}</dd></div>
                            <div><dt>Khởi chiếu</dt><dd>{movie.releaseDate || 'Đang cập nhật'}</dd></div>
                            <div><dt>Độ tuổi</dt><dd>{movie.ageRating || 'P'}</dd></div>
                        </dl>
                    </div>
                    <section className="content-card">
                        <div className="section-head">
                            <div><span className="section-kicker">KHÁN GIẢ</span><h2>Đánh giá</h2></div>
                            <div className="rating-hero">★ {movie.ratingAverage?.toFixed(1)}<small>/10</small></div>
                        </div>
                        <p className="muted">Tổng hợp từ {movie.reviewCount ?? 0} lượt đánh giá demo. Hệ thống review chi tiết nằm trong roadmap tiếp theo.</p>
                    </section>
                </section>
                <aside className="panel sticky showtime-panel">
                    <h3>Chọn suất chiếu</h3>
                    <p className="muted">Suất chiếu sắp tới</p>
                    {showtimes.length ? showtimes.slice(0, 6).map((showtime) => (
                        <Link key={showtime.id} className="time-chip" to={`/showtimes/${showtime.id}/seats`}>
                            <span>{showtime.cinemaName}</span><strong>{timeOnly(showtime.startTime)}</strong>
                        </Link>
                    )) : <p>Chưa có suất chiếu.</p>}
                    <Link className="btn btn-block" to={`/showtimes/select?movieId=${movie.id}`}>Xem toàn bộ lịch chiếu</Link>
                </aside>
            </div>

            {hotMovies.length > 0 && <section className="hot-movies-section">
                <div className="section-kicker">ĐANG ĐƯỢC QUAN TÂM</div>
                <div className="section-head">
                    <div><h2>Phim hot khác</h2><p>Khám phá thêm những bộ phim nổi bật tại CineVerse.</p></div>
                    <Link to="/movies">Xem tất cả</Link>
                </div>
                <div className="hot-movies-carousel">
                    <button className="carousel-arrow carousel-arrow-left" type="button" aria-label="Xem phim phía trước" onClick={() => scrollHotMovies(-1)}>‹</button>
                    <div
                        ref={hotMoviesRef}
                        className={`hot-movies-row ${draggingHotMovies ? 'is-dragging' : ''}`}
                        onPointerDown={startHotMoviesDrag}
                        onPointerMove={moveHotMoviesDrag}
                        onPointerUp={stopHotMoviesDrag}
                        onPointerCancel={stopHotMoviesDrag}
                        onClickCapture={(event) => {
                            if (!draggedRef.current) return;
                            event.preventDefault();
                            event.stopPropagation();
                            draggedRef.current = false;
                        }}
                    >
                        {hotMovies.map((item, index) => <div className="hot-movie-item" key={item.id}><MovieCard movie={item} index={index + 1} /></div>)}
                    </div>
                    <button className="carousel-arrow carousel-arrow-right" type="button" aria-label="Xem phim tiếp theo" onClick={() => scrollHotMovies(1)}>›</button>
                </div>
            </section>}
        </div>

        {trailerOpen && <div className="trailer-modal" role="dialog" aria-modal="true" aria-label={`Trailer ${movie.title}`} onMouseDown={(event) => { if (event.target === event.currentTarget) setTrailerOpen(false); }}>
            <div className="trailer-modal-dialog">
                <div className="trailer-modal-head">
                    <div><span className="section-kicker">TRAILER</span><h2>{movie.title}</h2></div>
                    <div className="trailer-modal-actions">
                        {movie.trailerUrl && <a target="_blank" rel="noreferrer" href={movie.trailerUrl}>Mở trên YouTube ↗</a>}
                        <button type="button" className="trailer-modal-close" aria-label="Đóng trailer" onClick={() => setTrailerOpen(false)}>×</button>
                    </div>
                </div>
                {embed ? <div className="video-frame"><iframe src={embed} title={`Trailer ${movie.title}`} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen /></div> : <div className="empty">Trailer đang cập nhật.</div>}
            </div>
        </div>}
    </>;
}