import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../lib/api';
import type { Movie, Showtime } from '../types';
import { BookingSteps } from '../components/BookingSteps';
import { timeOnly } from '../lib/format';
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
        {items.map((s) => (
          <div className="schedule-card" key={s.id}>
            <div>
              <h3>{s.cinemaName}</h3>
              <p>{s.roomName}</p>
            </div>
            <Link className="time-chip" to={`/showtimes/${s.id}/seats`}>
              {timeOnly(s.startTime)}
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
