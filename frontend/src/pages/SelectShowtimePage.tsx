import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { BookingSteps } from '../components/BookingSteps';
import { api } from '../lib/api';
import { dateTime, statusLabel } from '../lib/format';
import type { Movie, Showtime } from '../types';

export function SelectShowtimePage() {
    const [params] = useSearchParams();
    const movieId = params.get('movieId');
    const [movie, setMovie] = useState<Movie | null>(null);
    const [items, setItems] = useState<Showtime[]>([]);

    useEffect(() => {
        if (!movieId) return;
        void api<Movie>(`/movies/${movieId}`).then(setMovie);
        void api<Showtime[]>(`/showtimes?movieId=${movieId}`).then(setItems);
    }, [movieId]);

    return (
        <div className="container page">
            <BookingSteps active={1} />
            <div className="page-title">
                <h1>Chọn suất chiếu</h1>
                <p>{movie?.title ?? 'Chọn thời gian phù hợp.'}</p>
            </div>

            <div className="schedule-list">
                {items.map((showtime) => (
                    <div className="schedule-card" key={showtime.id}>
                        <div>
                            <h3>{showtime.cinemaName}</h3>
                            <p>{showtime.roomName} · {dateTime(showtime.startTime)}</p>
                            {showtime.lifecycleStatus === 'NOW_PLAYING' && (
                                <small className="showtime-live-note">
                                    {statusLabel(showtime.lifecycleStatus)} · còn nhận đặt vé trong thời gian giới hạn
                                </small>
                            )}
                        </div>
                        <Link className="time-chip" to={`/showtimes/${showtime.id}/seats`}>
                            Chọn ghế
                        </Link>
                    </div>
                ))}
                {items.length === 0 && (
                    <div className="empty">Hiện không còn suất chiếu nào đang mở bán vé.</div>
                )}
            </div>
        </div>
    );
}
