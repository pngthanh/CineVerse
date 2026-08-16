import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../lib/api';
import type { Movie, Showtime } from '../types';
import { timeOnly } from '../lib/format';

function youtubeEmbed(url?: string) {
    if (!url) return '';
    const match = url.match(/(?:v=|youtu\.be\/|embed\/)([A-Za-z0-9_-]{6,})/);
    return match ? `https://www.youtube.com/embed/${match[1]}` : '';
}

export function MovieDetailPage() {
    const { id } = useParams(); const navigate = useNavigate();
    const [movie, setMovie] = useState<Movie | null>(null); const [showtimes, setShowtimes] = useState<Showtime[]>([]);
    useEffect(() => { if (!id) return; void api<Movie>(`/movies/${id}`).then(setMovie); void api<Showtime[]>(`/showtimes?movieId=${id}`).then(setShowtimes).catch(() => setShowtimes([])); }, [id]);
    const embed = useMemo(() => youtubeEmbed(movie?.trailerUrl), [movie?.trailerUrl]);
    if (!movie) return <div className="page-center">Đang tải phim...</div>;
    return <>
        <section className="detail-hero" style={movie.backdropUrl ? { backgroundImage: `linear-gradient(90deg, rgba(8,10,18,.96), rgba(8,10,18,.70)), url(${movie.backdropUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : undefined}><div className="container"><button className="back-button" onClick={() => navigate(-1)}>← Trở về</button><div className="detail-layout"><div className={`detail-poster poster ${movie.posterUrl ? 'poster-image' : 'poster-space'}`} style={movie.posterUrl ? { backgroundImage:`url(${movie.posterUrl})` } : undefined}/><div><span className="eyebrow">{movie.status === 'NOW_SHOWING' ? 'ĐANG CHIẾU' : 'SẮP CHIẾU'}</span><h1>{movie.title}</h1><div className="detail-badges"><span>{movie.ageRating}</span><span>{movie.durationMinutes} phút</span><span>{movie.genres}</span></div><p>{movie.description}</p><div className="row">{movie.status === 'NOW_SHOWING' && <Link className="btn" to={`/showtimes/select?movieId=${movie.id}`}>Đặt vé</Link>}<a className="btn btn-secondary" href="#trailer">Xem trailer</a></div></div></div></div></section>
        <div className="container page"><div className="detail-content-grid"><section><div className="content-card"><span className="section-kicker">NỘI DUNG</span><h2>Giới thiệu phim</h2><p className="long-copy">{movie.description} CineVerse cập nhật lịch chiếu, thông tin phim và trạng thái ghế theo dữ liệu từ hệ thống.</p><dl className="details-list"><div><dt>Đạo diễn</dt><dd>{movie.director || 'Đang cập nhật'}</dd></div><div><dt>Diễn viên</dt><dd>{movie.castNames || 'Đang cập nhật'}</dd></div><div><dt>Thể loại</dt><dd>{movie.genres}</dd></div><div><dt>Khởi chiếu</dt><dd>{movie.releaseDate || 'Đang cập nhật'}</dd></div><div><dt>Độ tuổi</dt><dd>{movie.ageRating || 'P'}</dd></div></dl></div>
        <section id="trailer" className="content-card trailer-section"><div className="section-head"><div><span className="section-kicker">TRAILER</span><h2>Xem ngay tại CineVerse</h2></div>{movie.trailerUrl && <a target="_blank" rel="noreferrer" href={movie.trailerUrl}>Mở trên YouTube ↗</a>}</div>{embed ? <div className="video-frame"><iframe src={embed} title={`Trailer ${movie.title}`} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen /></div> : <div className="empty">Trailer đang cập nhật.</div>}</section></section>
        <aside className="panel sticky showtime-panel"><h3>Chọn suất chiếu</h3><p className="muted">Suất chiếu sắp tới</p>{showtimes.length ? showtimes.slice(0,6).map(s => <Link key={s.id} className="time-chip" to={`/showtimes/${s.id}/seats`}><span>{s.cinemaName}</span><strong>{timeOnly(s.startTime)}</strong></Link>) : <p>Chưa có suất chiếu.</p>}<Link className="btn btn-block" to={`/showtimes/select?movieId=${movie.id}`}>Xem toàn bộ lịch chiếu</Link></aside></div></div>
    </>;
}
