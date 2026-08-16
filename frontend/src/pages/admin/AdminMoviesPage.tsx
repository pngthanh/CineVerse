import { FormEvent, useEffect, useState } from 'react';
import { ApiError, api, apiUploadImage } from '../../lib/api';
import type { Movie, MovieStatus } from '../../types';

interface MovieForm {
  title: string;
  description: string;
  genres: string;
  durationMinutes: number;
  releaseDate: string;
  endDate: string;
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
  endDate: '',
  director: '',
  castNames: '',
  ageRating: 'T13',
  posterUrl: '',
  backdropUrl: '',
  trailerUrl: 'https://www.youtube.com/watch?v=0H_mDKTRVBQ',
  status: 'NOW_SHOWING',
});

export function AdminMoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [form, setForm] = useState<MovieForm>(createEmptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [uploading, setUploading] = useState<'poster' | 'backdrop' | null>(null);

  const load = async () => {
    const data = await api<Movie[]>('/admin/movies');
    setMovies(data);
  };

  useEffect(() => {
    let cancelled = false;

    api<Movie[]>('/admin/movies')
      .then((data) => {
        if (!cancelled) {
          setMovies(data);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Không thể tải danh sách phim.');
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const updateField = <K extends keyof MovieForm>(key: K, value: MovieForm[K]) => {
    setForm((current) => ({
      ...current,
      [key]: value,
    }));
  };

  const upload = async (kind: 'poster' | 'backdrop', file?: File) => {
    if (!file) return;

    setError('');
    setUploading(kind);

    try {
      const url = await apiUploadImage(file);

      updateField(kind === 'poster' ? 'posterUrl' : 'backdropUrl', url);
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'Không thể tải ảnh lên.');
    } finally {
      setUploading(null);
    }
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();

    setError('');
    setMessage('');

    try {
      await api(editingId ? `/admin/movies/${editingId}` : '/admin/movies', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify({
          ...form,
          endDate: form.endDate || null,
        }),
      });

      setMessage(editingId ? 'Đã cập nhật phim.' : 'Đã thêm phim mới.');

      setEditingId(null);
      setForm(createEmptyForm());

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
      releaseDate: movie.releaseDate ?? '',
      endDate: movie.endDate ?? '',
      director: movie.director ?? '',
      castNames: movie.castNames ?? '',
      ageRating: movie.ageRating ?? '',
      posterUrl: movie.posterUrl ?? '',
      backdropUrl: movie.backdropUrl ?? '',
      trailerUrl: movie.trailerUrl ?? 'https://www.youtube.com/watch?v=0H_mDKTRVBQ',
      status: movie.status,
    });

    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  };

  const deactivate = async (movie: Movie) => {
    if (movie.status === 'INACTIVE' || !window.confirm(`Vô hiệu hóa “${movie.title}”?`)) {
      return;
    }

    try {
      await api(`/admin/movies/${movie.id}`, {
        method: 'DELETE',
      });

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
        <p>Quản lý nội dung, thời gian phát hành, poster, banner và trailer YouTube.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {message && <div className="alert alert-success">{message}</div>}

      <div className="admin-grid admin-movie-grid">
        <form className="panel admin-movie-form" onSubmit={submit}>
          <h3>{editingId ? 'Chỉnh sửa phim' : 'Thêm phim'}</h3>

          <label>
            Tên phim
            <input
              value={form.title}
              onChange={(e) => updateField('title', e.target.value)}
              required
            />
          </label>

          <label>
            Mô tả
            <textarea
              value={form.description}
              onChange={(e) => updateField('description', e.target.value)}
              required
            />
          </label>

          <label>
            Thể loại
            <input
              value={form.genres}
              onChange={(e) => updateField('genres', e.target.value)}
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
                onChange={(e) => updateField('durationMinutes', Number(e.target.value))}
                required
              />
            </label>

            <label>
              Phân loại tuổi
              <input
                value={form.ageRating}
                onChange={(e) => updateField('ageRating', e.target.value)}
              />
            </label>
          </div>

          <div className="form-two-col">
            <label>
              Ngày bắt đầu chiếu
              <input
                type="date"
                value={form.releaseDate}
                onChange={(e) => updateField('releaseDate', e.target.value)}
              />
            </label>

            <label>
              Ngày dự kiến kết thúc
              <input
                type="date"
                value={form.endDate}
                onChange={(e) => updateField('endDate', e.target.value)}
              />
            </label>
          </div>

          <label>
            Đạo diễn
            <input
              value={form.director}
              onChange={(e) => updateField('director', e.target.value)}
            />
          </label>

          <label>
            Diễn viên
            <input
              value={form.castNames}
              onChange={(e) => updateField('castNames', e.target.value)}
            />
          </label>

          <div className="admin-media-fields">
            <label>
              Poster chính
              <input
                value={form.posterUrl}
                onChange={(e) => updateField('posterUrl', e.target.value)}
                placeholder="/posters/... hoặc upload ảnh"
              />
            </label>

            <label className="file-upload">
              Upload poster
              <input
                type="file"
                accept="image/*"
                onChange={(e) => void upload('poster', e.target.files?.[0])}
              />
              <span>{uploading === 'poster' ? 'Đang tải...' : 'Chọn ảnh poster'}</span>
            </label>

            {form.posterUrl && (
              <img
                className="admin-media-preview poster-preview"
                src={form.posterUrl}
                alt="Poster preview"
              />
            )}

            <label>
              Banner / backdrop
              <input
                value={form.backdropUrl}
                onChange={(e) => updateField('backdropUrl', e.target.value)}
                placeholder="/posters/... hoặc upload ảnh"
              />
            </label>

            <label className="file-upload">
              Upload banner
              <input
                type="file"
                accept="image/*"
                onChange={(e) => void upload('backdrop', e.target.files?.[0])}
              />
              <span>{uploading === 'backdrop' ? 'Đang tải...' : 'Chọn ảnh banner'}</span>
            </label>

            {form.backdropUrl && (
              <img
                className="admin-media-preview banner-preview"
                src={form.backdropUrl}
                alt="Banner preview"
              />
            )}
          </div>

          <label>
            Trailer YouTube
            <input
              value={form.trailerUrl}
              onChange={(e) => updateField('trailerUrl', e.target.value)}
              placeholder="https://www.youtube.com/watch?v=..."
            />
          </label>

          <label>
            Trạng thái
            <select
              value={form.status}
              onChange={(e) => updateField('status', e.target.value as MovieStatus)}
            >
              <option value="NOW_SHOWING">Đang chiếu</option>

              <option value="COMING_SOON">Sắp chiếu</option>

              <option value="INACTIVE">Không hoạt động</option>
            </select>
          </label>

          <div className="row">
            <button className="btn" disabled={uploading !== null}>
              {editingId ? 'Lưu thay đổi' : 'Thêm phim'}
            </button>

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
                <th>Thời gian</th>
                <th>Thể loại</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>

            <tbody>
              {movies.map((movie) => (
                <tr key={movie.id}>
                  <td>
                    <div className="admin-movie-cell">
                      {movie.posterUrl && <img src={movie.posterUrl} alt="" />}

                      <div>
                        <strong>{movie.title}</strong>

                        <small>
                          {movie.durationMinutes} phút · {movie.ageRating}
                        </small>
                      </div>
                    </div>
                  </td>

                  <td>
                    {movie.releaseDate ?? '—'}
                    <br />

                    <small>đến {movie.endDate ?? 'chưa đặt'}</small>
                  </td>

                  <td>{movie.genres}</td>

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
