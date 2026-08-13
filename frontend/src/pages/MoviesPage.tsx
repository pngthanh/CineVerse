import { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import type { Movie } from '../types';
import { MovieCard } from '../components/MovieCard';
export function MoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [tab, setTab] = useState<'NOW_SHOWING' | 'COMING_SOON'>('NOW_SHOWING');
  const [search, setSearch] = useState('');
  const [genre, setGenre] = useState('ALL');
  const [age, setAge] = useState('ALL');
  const [sort, setSort] = useState('POPULAR');
  useEffect(() => {
    void api<Movie[]>('/movies').then(setMovies);
  }, []);
  const genres = useMemo(
    () =>
      Array.from(new Set(movies.flatMap((m) => m.genres.split(',').map((g) => g.trim())))).sort(),
    [movies],
  );
  const filtered = useMemo(() => {
    const data = movies.filter(
      (m) =>
        m.status === tab &&
        m.title.toLowerCase().includes(search.toLowerCase()) &&
        (genre === 'ALL' || m.genres.includes(genre)) &&
        (age === 'ALL' || m.ageRating === age),
    );
    return [...data].sort((a, b) =>
      sort === 'RATING'
        ? (b.ratingAverage ?? 0) - (a.ratingAverage ?? 0)
        : sort === 'NEWEST'
          ? String(b.releaseDate).localeCompare(String(a.releaseDate))
          : (b.ticketsSold ?? 0) - (a.ticketsSold ?? 0),
    );
  }, [movies, tab, search, genre, age, sort]);
  return (
    <div className="container page">
      <div className="page-title movie-page-title">
        <span className="eyebrow">CINEVERSE MOVIES</span>
        <h1>Khám phá phim</h1>
        <p>Tìm phim theo trạng thái, thể loại, độ tuổi và mức độ yêu thích.</p>
      </div>
      <div className="tabs">
        <button
          className={tab === 'NOW_SHOWING' ? 'active' : ''}
          onClick={() => setTab('NOW_SHOWING')}
        >
          Đang chiếu
        </button>
        <button
          className={tab === 'COMING_SOON' ? 'active' : ''}
          onClick={() => setTab('COMING_SOON')}
        >
          Sắp chiếu
        </button>
      </div>
      <div className="filter-bar filter-grid">
        <input
          placeholder="Tìm tên phim..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select value={genre} onChange={(e) => setGenre(e.target.value)}>
          <option value="ALL">Tất cả thể loại</option>
          {genres.map((g) => (
            <option key={g}>{g}</option>
          ))}
        </select>
        <select value={age} onChange={(e) => setAge(e.target.value)}>
          <option value="ALL">Mọi độ tuổi</option>
          <option>T13</option>
          <option>T16</option>
          <option>T18</option>
          <option>P</option>
        </select>
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="POPULAR">Phổ biến nhất</option>
          <option value="RATING">Đánh giá cao</option>
          <option value="NEWEST">Mới nhất</option>
        </select>
      </div>
      <div className="results-head">
        <strong>{filtered.length} phim</strong>
        <span>Cập nhật từ hệ thống CineVerse</span>
      </div>
      <div className="movie-grid">
        {filtered.map((m, i) => (
          <MovieCard key={m.id} movie={m} index={i} />
        ))}
      </div>
      {!filtered.length && <div className="empty">Không tìm thấy phim phù hợp.</div>}
    </div>
  );
}
