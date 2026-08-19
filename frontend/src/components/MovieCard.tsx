import { Link } from 'react-router-dom';
import type { Movie } from '../types';

const posters = ['poster-space', 'poster-desert', 'poster-dream', 'poster-fire'];

export function MovieCard({ movie, index = 0 }: { movie: Movie; index?: number }) {
    const posterStyle = movie.posterUrl ? { backgroundImage: `url(${movie.posterUrl})` } : undefined;
    return (
        <article className="movie-card">
            <Link
                className={`poster ${movie.posterUrl ? 'poster-image' : posters[index % posters.length]}`}
                style={posterStyle}
                to={`/movies/${movie.id}`}
            >
                <span className="rating-badge">{movie.ageRating ?? 'P'}</span>
            </Link>
            <div className="movie-info">
                <div className="movie-title-slot"><h3>{movie.title}</h3></div>
                <div className="movie-meta-slot">
                    <p>{movie.genres}</p>
                    <p>{movie.durationMinutes} phút · {movie.ageRating ?? 'P'}</p>
                </div>
                <div className="movie-action-slot">
                    {movie.status === 'NOW_SHOWING' ? (
                        <Link className="btn btn-block" to={`/movies/${movie.id}`}>Đặt vé</Link>
                    ) : (
                        <Link className="btn btn-secondary btn-block" to={`/movies/${movie.id}`}>Xem chi tiết</Link>
                    )}
                </div>
            </div>
        </article>
    );
}
