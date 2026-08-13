import { FormEvent, useEffect, useState } from 'react';
import { ApiError, api } from '../../lib/api';
import type { Movie, MovieStatus } from '../../types';
interface MovieForm {
  title: string;
  description: string;
  genres: string;
  durationMinutes: number;
  releaseDate: string;
  director: string;
  castNames: string;
  ageRating: string;
  posterUrl: string;
  backdropUrl: string;
  trailerUrl: string;
  status: MovieStatus;
}
const createEmptyForm = (): MovieForm => ({
  title: '',
  description: '',
  genres: '',
  durationMinutes: 120,
  releaseDate: new Date().toISOString().slice(0, 10),
  director: '',
  castNames: '',
  ageRating: 'T13',
  posterUrl: '',
  backdropUrl: '',
  trailerUrl: '',
  status: 'NOW_SHOWING',
});
export function AdminMoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [form, setForm] = useState<MovieForm>(createEmptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const load = async () => setMovies(await api<Movie[]>('/admin/movies'));

  useEffect(() => {
    let active = true;
    void api<Movie[]>('/admin/movies')
      .then((data) => {
        if (active) setMovies(data);
      })
      .catch(() => {
        if (active) setError('Không thể tải danh sách phim.');
      });

    return () => {
      active = false;
    };
  }, []);
  const updateField = <K extends keyof MovieForm>(key: K, value: MovieForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setMessage('');
    try {
      await api(editingId ? `/admin/movies/${editingId}` : '/admin/movies', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(form),
      });
      setForm(createEmptyForm());
      setEditingId(null);
      setMessage(editingId ? 'Đã cập nhật phim.' : 'Đã thêm phim mới.');
      await load();
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'Không thể lưu phim.');
    }
  };
  const startEdit = (movie: Movie) => {
    setEditingId(movie.id);
    setForm({
      title: movie.title,
      description: movie.description,
      genres: movie.genres,
      durationMinutes: movie.durationMinutes,
      releaseDate: movie.releaseDate ?? new Date().toISOString().slice(0, 10),
      director: movie.director ?? '',
      castNames: movie.castNames ?? '',
      ageRating: movie.ageRating ?? '',
      posterUrl: movie.posterUrl ?? '',
      backdropUrl: movie.backdropUrl ?? '',
      trailerUrl: movie.trailerUrl ?? '',
      status: movie.status,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  const deactivate = async (movie: Movie) => {
    if (movie.status === 'INACTIVE') return;
    if (!window.confirm(`Chuyển “${movie.title}” sang trạng thái không hoạt động?`)) return;
    try {
      await api(`/admin/movies/${movie.id}`, { method: 'DELETE' });
      await load();
    } catch (requestError) {
      setError(
        requestError instanceof ApiError ? requestError.message : 'Không thể cập nhật phim.',
      );
    }
  };
  return (
    <div className="admin-page">
      <div className="page-title">
        <h1>Quản lý phim</h1>
        <p>Thêm, chỉnh sửa và vô hiệu hóa phim mà không xóa lịch sử.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <div className="admin-grid">
        <form className="panel" onSubmit={submit}>
          <h3>{editingId ? 'Chỉnh sửa phim' : 'Thêm phim'}</h3>
          <label>
            Tên phim
            <input
              value={form.title}
              onChange={(event) => updateField('title', event.target.value)}
              required
            />
          </label>
          <label>
            Mô tả
            <textarea
              value={form.description}
              onChange={(event) => updateField('description', event.target.value)}
              required
            />
          </label>
          <label>
            Thể loại
            <input
              value={form.genres}
              onChange={(event) => updateField('genres', event.target.value)}
              required
            />
          </label>
          <div className="form-two-col">
            <label>
              Thời lượng (phút)
              <input
                type="number"
                min="1"
                value={form.durationMinutes}
                onChange={(event) => updateField('durationMinutes', Number(event.target.value))}
                required
              />
            </label>
            <label>
              Phân loại tuổi
              <input
                value={form.ageRating}
                onChange={(event) => updateField('ageRating', event.target.value)}
              />
            </label>
          </div>
          <label>
            Ngày phát hành
            <input
              type="date"
              value={form.releaseDate}
              onChange={(event) => updateField('releaseDate', event.target.value)}
            />
          </label>
          <label>
            Đạo diễn
            <input
              value={form.director}
              onChange={(event) => updateField('director', event.target.value)}
            />
          </label>
          <label>
            Diễn viên
            <input
              value={form.castNames}
              onChange={(event) => updateField('castNames', event.target.value)}
            />
          </label>
          <label>
            Trạng thái
            <select
              value={form.status}
              onChange={(event) => updateField('status', event.target.value as MovieStatus)}
            >
              <option value="NOW_SHOWING">Đang chiếu</option>
              <option value="COMING_SOON">Sắp chiếu</option>
              <option value="INACTIVE">Không hoạt động</option>
            </select>
          </label>
          <div className="row">
            <button className="btn">{editingId ? 'Lưu thay đổi' : 'Thêm phim'}</button>
            {editingId && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setEditingId(null);
                  setForm(createEmptyForm());
                }}
              >
                Hủy sửa
              </button>
            )}
          </div>
        </form>

        <section className="panel table-wrap">
          <table>
            <thead>
              <tr>
                <th>Phim</th>
                <th>Thể loại</th>
                <th>Thời lượng</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {movies.map((movie) => (
                <tr key={movie.id}>
                  <td>
                    <strong>{movie.title}</strong>
                  </td>
                  <td>{movie.genres}</td>
                  <td>{movie.durationMinutes} phút</td>
                  <td>{movie.status}</td>
                  <td>
                    <div className="row table-actions">
                      <button className="btn btn-sm btn-secondary" onClick={() => startEdit(movie)}>
                        Sửa
                      </button>
                      <button
                        className="btn btn-sm btn-danger"
                        disabled={movie.status === 'INACTIVE'}
                        onClick={() => void deactivate(movie)}
                      >
                        Vô hiệu hóa
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </div>
  );
}
