import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import type { Cinema, Movie } from '../types';
import { MovieCard } from '../components/MovieCard';
export function HomePage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [cinemas, setCinemas] = useState<Cinema[]>([]);
  useEffect(() => {
    void api<Movie[]>('/movies').then(setMovies);
    void api<Cinema[]>('/cinemas').then(setCinemas);
  }, []);
  const now = movies.filter((m) => m.status === 'NOW_SHOWING');
  const soon = movies.filter((m) => m.status === 'COMING_SOON');
  const ranked = useMemo(
    () =>
      [...now].sort(
        (a, b) =>
          (b.ticketsSold ?? 0) - (a.ticketsSold ?? 0) ||
          (b.ratingAverage ?? 0) - (a.ratingAverage ?? 0),
      ),
    [now],
  );
  const hero = ranked[0];
  const topRated = useMemo(
    () => [...movies].sort((a, b) => (b.ratingAverage ?? 0) - (a.ratingAverage ?? 0)).slice(0, 5),
    [movies],
  );
  return (
    <>
      <section
        className="hero"
        style={
          hero?.backdropUrl
            ? {
                backgroundImage: `linear-gradient(90deg,rgba(7,9,14,.97) 5%,rgba(7,9,14,.72) 46%,rgba(7,9,14,.2)),url(${hero.backdropUrl})`,
              }
            : undefined
        }
      >
        <div className="hero-art" />
        <div className="container hero-content">
          <span className="eyebrow">NỔI BẬT NHẤT HÔM NAY</span>
          <h1>{hero?.title ?? 'CineVerse'}</h1>
          <p>{hero?.description ?? 'Khám phá thế giới điện ảnh.'}</p>
          {hero && (
            <div className="hero-stats">
              <span>★ {hero.ratingAverage?.toFixed(1)}</span>
              <span>{hero.reviewCount} đánh giá</span>
              <span>{hero.ticketsSold} vé đã bán</span>
              <span>{hero.durationMinutes} phút</span>
            </div>
          )}
          <div className="row">
            <Link className="btn" to={hero ? `/movies/${hero.id}` : '/movies'}>
              Đặt vé ngay
            </Link>
            <Link className="btn btn-secondary" to="/movies">
              Khám phá phim
            </Link>
          </div>
        </div>
      </section>
      <section className="container section">
        <div className="section-kicker">ĐƯỢC YÊU THÍCH</div>
        <div className="section-head">
          <div>
            <h2>Top đánh giá</h2>
            <p>5 bộ phim được khán giả đánh giá cao nhất.</p>
          </div>
          <Link to="/movies">Xem tất cả</Link>
        </div>
        <div className="movie-grid">
          {topRated.map((m, i) => (
            <MovieCard key={m.id} movie={m} index={i} />
          ))}
        </div>
      </section>
      <section className="section section-tinted">
        <div className="container">
          <div className="section-head">
            <div>
              <h2>Phim đang chiếu</h2>
              <p>Chọn suất chiếu phù hợp và giữ ghế theo thời gian thực.</p>
            </div>
            <Link to="/movies">Toàn bộ phim</Link>
          </div>
          <div className="movie-grid">
            {now.slice(0, 5).map((m, i) => (
              <MovieCard key={m.id} movie={m} index={i} />
            ))}
          </div>
        </div>
      </section>
      <section className="container section">
        <div className="section-head">
          <div>
            <h2>Sắp chiếu</h2>
            <p>Những tựa phim đang chờ ngày ra mắt.</p>
          </div>
        </div>
        <div className="movie-grid">
          {soon.slice(0, 5).map((m, i) => (
            <MovieCard key={m.id} movie={m} index={i + 2} />
          ))}
        </div>
      </section>
      <section className="section section-tinted">
        <div className="container">
          <div className="section-head">
            <div>
              <h2>Hệ thống rạp</h2>
              <p>Không gian xem phim hiện đại, lịch chiếu rõ ràng.</p>
            </div>
            <Link to="/cinemas">Xem rạp</Link>
          </div>
          <div className="cinema-grid">
            {cinemas.map((c) => (
              <Link to={`/cinemas/${c.id}`} key={c.id} className="cinema-card">
                <span>RẠP CINEVERSE</span>
                <h3>{c.name}</h3>
                <p>{c.address}</p>
                <small>{c.rooms.length} phòng chiếu</small>
                <b>Khám phá rạp →</b>
              </Link>
            ))}
          </div>
        </div>
      </section>
      <section className="container section experience-section">
        <div className="experience-heading">
          <div>
            <span className="section-kicker">TRẢI NGHIỆM ĐẶT VÉ</span>
            <h2>Đặt vé nhanh, giữ ghế rõ ràng</h2>
            <p>Từ lúc chọn ghế đến khi nhận vé, CineVerse giữ mọi bước gọn gàng và minh bạch.</p>
          </div>
          <Link className="experience-link" to="/movies">
            Bắt đầu đặt vé <span>→</span>
          </Link>
        </div>
        <div className="experience-grid">
          <article className="experience-card">
            <div className="experience-card-top">
              <span className="experience-icon" aria-hidden="true">
                ▦
              </span>
              <span className="experience-index">01</span>
            </div>
            <h3>Chọn ghế trực quan</h3>
            <p>Sơ đồ ghế cập nhật trạng thái từ máy chủ để bạn biết ngay ghế nào còn trống.</p>
            <div className="experience-meta">
              <span className="experience-dot" /> Cập nhật theo thời gian thực
            </div>
          </article>
          <article className="experience-card">
            <div className="experience-card-top">
              <span className="experience-icon" aria-hidden="true">
                ◈
              </span>
              <span className="experience-index">02</span>
            </div>
            <h3>Giữ ghế an toàn</h3>
            <p>Ghế được giữ tạm trong lúc hoàn tất booking, hạn chế tối đa tình trạng đặt trùng.</p>
            <div className="experience-meta">
              <span className="experience-dot" /> Có thời gian giữ rõ ràng
            </div>
          </article>
          <article className="experience-card">
            <div className="experience-card-top">
              <span className="experience-icon" aria-hidden="true">
                ✓
              </span>
              <span className="experience-index">03</span>
            </div>
            <h3>Vé tức thì</h3>
            <p>Thanh toán thành công là có mã vé ngay, sẵn sàng xem lại trong mục Vé của tôi.</p>
            <div className="experience-meta">
              <span className="experience-dot" /> Nhận vé ngay sau thanh toán
            </div>
          </article>
        </div>
      </section>
    </>
  );
}
