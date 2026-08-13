import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../lib/api';
import { timeOnly } from '../lib/format';
import type { Cinema, Movie, Showtime } from '../types';

export function CinemaDetailPage() {
    const { id } = useParams();
    const [cinema, setCinema] = useState<Cinema | null>(null);
    const [times, setTimes] = useState<Showtime[]>([]);
    const [movies, setMovies] = useState<Movie[]>([]);

    useEffect(() => {
        if (!id) return;
        void Promise.all([
            api<Cinema>(`/cinemas/${id}`),
            api<Showtime[]>(`/showtimes?cinemaId=${id}`),
            api<Movie[]>('/movies'),
        ]).then(([cinemaData, showtimeData, movieData]) => {
            setCinema(cinemaData);
            setTimes(showtimeData);
            setMovies(movieData);
        });
    }, [id]);

    const grouped = useMemo(() => {
        return times.reduce<Map<number, Showtime[]>>((map, showtime) => {
            const items = map.get(showtime.movieId) ?? [];
            items.push(showtime);
            map.set(showtime.movieId, items);
            return map;
        }, new Map());
    }, [times]);

    if (!cinema) return <div className="page-center">Đang tải...</div>;

    return (
        <div className="container page cinema-detail-page">
            <div className="page-title cinema-detail-heading">
                <span className="eyebrow">RẠP CINEVERSE</span>
                <h1>{cinema.name}</h1>
                <p>{cinema.address} · {cinema.rooms.length} phòng chiếu</p>
            </div>

            <div className="cinema-movie-schedule-list">
                {[...grouped.entries()].map(([movieId, items]) => {
                    const movie = movies.find((item) => item.id === movieId);
                    return (
                        <article className="cinema-movie-schedule" key={movieId}>
                            <Link
                                to={`/movies/${movieId}`}
                                className="cinema-schedule-poster"
                                style={movie?.posterUrl ? { backgroundImage: `url(${movie.posterUrl})` } : undefined}
                            >
                                {!movie?.posterUrl && <span>{movie?.title.slice(0, 1) ?? 'C'}</span>}
                            </Link>
                            <div className="cinema-schedule-main">
                                <div className="cinema-schedule-title">
                                    <div>
                                        <span className="eyebrow">ĐANG CÓ SUẤT</span>
                                        <h2>{movie?.title ?? items[0].movieTitle}</h2>
                                        <p>{movie?.genres ?? 'Phim CineVerse'} · {movie?.durationMinutes ?? '—'} phút · {movie?.ageRating ?? 'P'}</p>
                                    </div>
                                    <Link to={`/movies/${movieId}`}>Xem phim →</Link>
                                </div>
                                <div className="cinema-schedule-times">
                                    {items.map((showtime) => (
                                        <Link className="cinema-time-card" key={showtime.id} to={`/showtimes/${showtime.id}/seats`}>
                                            <strong>{timeOnly(showtime.startTime)}</strong>
                                            <span>{showtime.roomName}</span>
                                        </Link>
                                    ))}
                                </div>
                            </div>
                        </article>
                    );
                })}
                {times.length === 0 && <div className="empty panel">Rạp hiện chưa có suất chiếu.</div>}
            </div>
        </div>
    );
}
