import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { MovieCard } from '../components/MovieCard';
import { api } from '../lib/api';
import type { Cinema, Movie } from '../types';

export function HomePage() {
    const [movies, setMovies] = useState<Movie[]>([]);
    const [cinemas, setCinemas] = useState<Cinema[]>([]);

    useEffect(() => {
        void api<Movie[]>('/movies').then(setMovies);
        void api<Cinema[]>('/cinemas').then(setCinemas);
    }, []);

    const now = movies.filter((movie) => movie.status === 'NOW_SHOWING');
    const soon = movies.filter((movie) => movie.status === 'COMING_SOON');
    const ranked = useMemo(
        () => [...now].sort((a, b) => (b.ticketsSold ?? 0) - (a.ticketsSold ?? 0)),
        [now],
    );
    const hero = ranked[0];
    const popular = ranked.slice(0, 5);

    return <>
        <section
            className="hero"
            style={hero?.backdropUrl ? {
                backgroundImage:
                    `linear-gradient(90deg,rgba(7,9,14,.97) 5%,rgba(7,9,14,.72) 46%,rgba(7,9,14,.2)),url(${hero.backdropUrl})`,
            } : undefined}
        >
            <div className="hero-art" />
            <div className="container hero-content">
                <span className="eyebrow">NỔI BẬT NHẤT HÔM NAY</span>
                <h1>{hero?.title ?? 'CineVerse'}</h1>
                <p>{hero?.description ?? 'Khám phá thế giới điện ảnh.'}</p>
                {hero && <div className="hero-stats">
                    <span>{hero.ticketsSold ?? 0} vé đã bán</span>
                    <span>{hero.durationMinutes} phút</span>
                    <span>{hero.ageRating ?? 'P'}</span>
                </div>}
                <div className="row">
                    <Link className="btn" to={hero ? `/movies/${hero.id}` : '/movies'}>Đặt vé ngay</Link>
                    <Link className="btn btn-secondary" to="/movies">Khám phá phim</Link>
                </div>
            </div>
        </section>
        <section className="container section">
            <div className="section-kicker">ĐANG ĐƯỢC QUAN TÂM</div>
            <div className="section-head">
                <div><h2>Phim bán vé nổi bật</h2><p>Các phim đang được đặt vé nhiều trên CineVerse.</p></div>
                <Link to="/movies">Xem tất cả</Link>
            </div>
            <div className="movie-grid">{popular.map((movie, index) => (
                <MovieCard key={movie.id} movie={movie} index={index} />
            ))}</div>
        </section>
        <section className="section section-tinted">
            <div className="container">
                <div className="section-head">
                    <div><h2>Phim đang chiếu</h2><p>Chọn suất chiếu phù hợp và giữ ghế theo thời gian thực.</p></div>
                    <Link to="/movies">Toàn bộ phim</Link>
                </div>
                <div className="movie-grid">{now.slice(0, 5).map((movie, index) => (
                    <MovieCard key={movie.id} movie={movie} index={index} />
                ))}</div>
            </div>
        </section>
        <section className="container section">
            <div className="section-head"><div><h2>Sắp chiếu</h2><p>Những tựa phim đang chờ ngày ra mắt.</p></div></div>
            <div className="movie-grid">{soon.slice(0, 5).map((movie, index) => (
                <MovieCard key={movie.id} movie={movie} index={index + 2} />
            ))}</div>
        </section>
        <section className="section section-tinted">
            <div className="container">
                <div className="section-head">
                    <div><h2>Hệ thống rạp</h2><p>Không gian xem phim hiện đại, lịch chiếu rõ ràng.</p></div>
                    <Link to="/cinemas">Xem rạp</Link>
                </div>
                <div className="cinema-grid">{cinemas.map((cinema) => (
                    <Link to={`/cinemas/${cinema.id}`} key={cinema.id} className="cinema-card">
                        <span>RẠP CINEVERSE</span><h3>{cinema.name}</h3><p>{cinema.address}</p>
                        <small>{cinema.rooms.length} phòng chiếu</small><b>Khám phá rạp →</b>
                    </Link>
                ))}</div>
            </div>
        </section>
        <section className="container experience-strip section">
            <div><span>01</span><strong>Chọn ghế trực quan</strong><p>Sơ đồ ghế cập nhật trạng thái từ máy chủ.</p></div>
            <div><span>02</span><strong>Giữ ghế an toàn</strong><p>Chống đặt trùng ghế và có thời gian giữ rõ ràng.</p></div>
            <div><span>03</span><strong>Vé tức thì</strong><p>Nhận mã vé ngay khi thanh toán thành công.</p></div>
        </section>
    </>;
}
