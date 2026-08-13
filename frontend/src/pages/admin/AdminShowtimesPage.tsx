import { FormEvent, useEffect, useMemo, useState } from 'react';
import { ApiError, api } from '../../lib/api';
import { dateTime, money, timeOnly } from '../../lib/format';
import type { Cinema, Movie, Showtime } from '../../types';

export function AdminShowtimesPage() {
    const [movies, setMovies] = useState<Movie[]>([]);
    const [cinemas, setCinemas] = useState<Cinema[]>([]);
    const [showtimes, setShowtimes] = useState<Showtime[]>([]);
    const [cinemaId, setCinemaId] = useState('');
    const [movieId, setMovieId] = useState('');
    const [roomId, setRoomId] = useState('');
    const [startTime, setStartTime] = useState('');
    const [basePrice, setBasePrice] = useState(70000);
    const [filterMovieId, setFilterMovieId] = useState('');
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    useEffect(() => {
        void Promise.all([api<Movie[]>('/movies'), api<Cinema[]>('/cinemas')])
            .then(([movieData, cinemaData]) => {
                setMovies(movieData);
                setCinemas(cinemaData);
                if (movieData[0]) setMovieId(String(movieData[0].id));
                if (cinemaData[0]) {
                    setCinemaId(String(cinemaData[0].id));
                    if (cinemaData[0].rooms[0]) setRoomId(String(cinemaData[0].rooms[0].id));
                }
            })
            .catch(() => setError('Không thể tải dữ liệu tạo suất chiếu.'));
    }, []);

    const selectedCinema = useMemo(
        () => cinemas.find((cinema) => cinema.id === Number(cinemaId)),
        [cinemas, cinemaId],
    );

    useEffect(() => {
        if (!cinemaId) return;
        void api<Showtime[]>(`/showtimes?cinemaId=${cinemaId}`)
            .then(setShowtimes)
            .catch(() => setError('Không thể tải lịch chiếu.'));
    }, [cinemaId]);

    const handleCinemaChange = (nextCinemaId: string) => {
        setCinemaId(nextCinemaId);
        const nextCinema = cinemas.find((cinema) => cinema.id === Number(nextCinemaId));
        setRoomId(nextCinema?.rooms[0] ? String(nextCinema.rooms[0].id) : '');
    };

    const reload = async () => {
        if (!cinemaId) return;
        setShowtimes(await api<Showtime[]>(`/showtimes?cinemaId=${cinemaId}`));
    };

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        setMessage('');
        try {
            await api('/admin/showtimes', {
                method: 'POST',
                body: JSON.stringify({
                    movieId: Number(movieId),
                    roomId: Number(roomId),
                    startTime,
                    basePrice,
                }),
            });
            setStartTime('');
            setMessage('Đã tạo suất chiếu.');
            await reload();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể tạo suất chiếu.');
        }
    };

    const cancel = async (showtime: Showtime) => {
        if (!window.confirm(`Hủy suất ${showtime.movieTitle} lúc ${dateTime(showtime.startTime)}?`)) return;
        try {
            await api(`/admin/showtimes/${showtime.id}`, { method: 'DELETE' });
            await reload();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể hủy suất chiếu.');
        }
    };

    const visibleShowtimes = filterMovieId
        ? showtimes.filter((showtime) => showtime.movieId === Number(filterMovieId))
        : showtimes;

    const grouped = visibleShowtimes.reduce<Map<number, Showtime[]>>((map, showtime) => {
        const items = map.get(showtime.movieId) ?? [];
        items.push(showtime);
        map.set(showtime.movieId, items);
        return map;
    }, new Map());

    return (
        <div className="admin-page">
            <div className="page-title">
                <h1>Suất chiếu</h1>
                <p>Máy chủ kiểm tra và chặn lịch bị chồng trong cùng một phòng.</p>
            </div>

            {error && <div className="alert alert-error">{error}</div>}
            {message && <div className="alert alert-success">{message}</div>}

            <div className="admin-grid">
                <form className="panel" onSubmit={submit}>
                    <h3>Tạo suất chiếu</h3>
                    <label>
                        Rạp
                        <select value={cinemaId} onChange={(event) => handleCinemaChange(event.target.value)} required>
                            {cinemas.map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}
                        </select>
                    </label>
                    <label>
                        Phim
                        <select value={movieId} onChange={(event) => setMovieId(event.target.value)} required>
                            <option value="">Chọn phim</option>
                            {movies.filter((movie) => movie.status !== 'INACTIVE').map((movie) => (
                                <option value={movie.id} key={movie.id}>{movie.title}</option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Phòng
                        <select value={roomId} onChange={(event) => setRoomId(event.target.value)} required>
                            {(selectedCinema?.rooms ?? []).map((room) => <option value={room.id} key={room.id}>{room.name}</option>)}
                        </select>
                    </label>
                    <label>
                        Thời gian bắt đầu
                        <input type="datetime-local" value={startTime} onChange={(event) => setStartTime(event.target.value)} required />
                    </label>
                    <label>
                        Giá cơ bản
                        <input type="number" min="0" step="1000" value={basePrice} onChange={(event) => setBasePrice(Number(event.target.value))} required />
                    </label>
                    <button className="btn" disabled={!roomId}>Tạo suất</button>
                </form>

                <section className="panel admin-showtime-panel">
                    <div className="section-head compact admin-showtime-head">
                        <div>
                            <h2>Lịch chiếu theo phim</h2>
                            <p>{selectedCinema?.name ?? 'Chưa chọn rạp'}</p>
                        </div>
                        <select value={filterMovieId} onChange={(event) => setFilterMovieId(event.target.value)}>
                            <option value="">Tất cả phim</option>
                            {movies.map((movie) => <option key={movie.id} value={movie.id}>{movie.title}</option>)}
                        </select>
                    </div>

                    <div className="admin-movie-showtime-list">
                        {[...grouped.entries()].map(([groupMovieId, items]) => {
                            const movie = movies.find((item) => item.id === groupMovieId);
                            return (
                                <article className="admin-movie-showtime-group" key={groupMovieId}>
                                    <div
                                        className="admin-movie-thumb"
                                        style={movie?.posterUrl ? { backgroundImage: `url(${movie.posterUrl})` } : undefined}
                                    >
                                        {!movie?.posterUrl && <span>{movie?.title.slice(0, 1) ?? 'C'}</span>}
                                    </div>
                                    <div className="admin-movie-showtime-body">
                                        <div className="admin-movie-showtime-title">
                                            <div>
                                                <h3>{movie?.title ?? items[0].movieTitle}</h3>
                                                <p>{movie?.genres ?? 'Phim CineVerse'} · {movie?.durationMinutes ?? '—'} phút</p>
                                            </div>
                                            <span>{items.length} suất</span>
                                        </div>
                                        <div className="admin-showtime-chips">
                                            {items.map((showtime) => (
                                                <div className="admin-showtime-chip" key={showtime.id}>
                                                    <strong>{timeOnly(showtime.startTime)}</strong>
                                                    <span>{showtime.roomName}</span>
                                                    <small>{money(showtime.basePrice)}</small>
                                                    <button type="button" onClick={() => void cancel(showtime)}>Hủy</button>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                </article>
                            );
                        })}
                        {visibleShowtimes.length === 0 && <div className="empty">Chưa có suất chiếu phù hợp.</div>}
                    </div>
                </section>
            </div>
        </div>
    );
}
