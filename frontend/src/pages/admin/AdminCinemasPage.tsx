import { FormEvent, useEffect, useMemo, useState } from 'react';
import { ApiError, api } from '../../lib/api';
import { money } from '../../lib/format';
import type { Cinema, CinemaRoomSummary } from '../../types';

type CinemaForm = {
  name: string;
  address: string;
  active: boolean;
};

type RoomForm = {
  name: string;
  rows: number;
  seatsPerRow: number;
  weekdayBasePrice: number;
  weekendBasePrice: number;
  vipSurcharge: number;
  active: boolean;
};

const emptyCinema: CinemaForm = { name: '', address: '', active: true };
const emptyRoom: RoomForm = {
  name: '',
  rows: 8,
  seatsPerRow: 10,
  weekdayBasePrice: 70000,
  weekendBasePrice: 100000,
  vipSurcharge: 20000,
  active: true,
};

function roomToForm(room: CinemaRoomSummary): RoomForm {
  return {
    name: room.name,
    rows: room.rows,
    seatsPerRow: room.seatsPerRow,
    weekdayBasePrice: room.weekdayBasePrice,
    weekendBasePrice: room.weekendBasePrice,
    vipSurcharge: room.vipSurcharge,
    active: room.active,
  };
}

export function AdminCinemasPage() {
  const [cinemas, setCinemas] = useState<Cinema[]>([]);
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [cinemaForm, setCinemaForm] = useState<CinemaForm>(emptyCinema);
  const [editingCinemaId, setEditingCinemaId] = useState<number | null>(null);
  const [roomForm, setRoomForm] = useState<RoomForm>(emptyRoom);
  const [editingRoomId, setEditingRoomId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = async () => {
    const data = await api<Cinema[]>('/admin/cinemas');
    setCinemas(data);
    setSelectedCinemaId((current) => {
      if (current && data.some((cinema) => cinema.id === Number(current))) return current;
      return data[0] ? String(data[0].id) : '';
    });
  };

  useEffect(() => {
    let active = true;
    void api<Cinema[]>('/admin/cinemas')
      .then((data) => {
        if (!active) return;
        setCinemas(data);
        if (data[0]) setSelectedCinemaId(String(data[0].id));
      })
      .catch(() => {
        if (active) setError('Không thể tải danh sách rạp.');
      });
    return () => {
      active = false;
    };
  }, []);

  const selectedCinema = useMemo(
    () => cinemas.find((cinema) => cinema.id === Number(selectedCinemaId)),
    [cinemas, selectedCinemaId],
  );

  const submitCinema = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setMessage('');
    try {
      if (editingCinemaId) {
        await api(`/admin/cinemas/${editingCinemaId}`, {
          method: 'PUT',
          body: JSON.stringify(cinemaForm),
        });
        setMessage('Đã cập nhật rạp.');
      } else {
        await api('/admin/cinemas', {
          method: 'POST',
          body: JSON.stringify(cinemaForm),
        });
        setMessage('Đã tạo rạp mới.');
      }
      setCinemaForm(emptyCinema);
      setEditingCinemaId(null);
      await load();
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'Không thể lưu rạp.');
    }
  };

  const editCinema = (cinema: Cinema) => {
    setEditingCinemaId(cinema.id);
    setCinemaForm({ name: cinema.name, address: cinema.address, active: cinema.active });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const deactivateCinema = async (cinema: Cinema) => {
    if (!window.confirm(`Ngừng hoạt động rạp “${cinema.name}”?`)) return;
    setError('');
    setMessage('');
    try {
      await api(`/admin/cinemas/${cinema.id}`, { method: 'DELETE' });
      setMessage('Rạp đã được ngừng hoạt động. Dữ liệu lịch sử vẫn được giữ nguyên.');
      await load();
    } catch (requestError) {
      setError(
        requestError instanceof ApiError ? requestError.message : 'Không thể ngừng hoạt động rạp.',
      );
    }
  };

  const submitRoom = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedCinemaId) return;
    setError('');
    setMessage('');
    try {
      if (editingRoomId) {
        await api(`/admin/rooms/${editingRoomId}`, {
          method: 'PUT',
          body: JSON.stringify(roomForm),
        });
        setMessage('Đã cập nhật phòng chiếu.');
      } else {
        await api(`/admin/cinemas/${selectedCinemaId}/rooms`, {
          method: 'POST',
          body: JSON.stringify(roomForm),
        });
        setMessage('Đã tạo phòng và sơ đồ ghế tự động.');
      }
      setRoomForm(emptyRoom);
      setEditingRoomId(null);
      await load();
    } catch (requestError) {
      setError(
        requestError instanceof ApiError ? requestError.message : 'Không thể lưu phòng chiếu.',
      );
    }
  };

  const editRoom = (cinema: Cinema, room: CinemaRoomSummary) => {
    setSelectedCinemaId(String(cinema.id));
    setEditingRoomId(room.id);
    setRoomForm(roomToForm(room));
    window.scrollTo({ top: 300, behavior: 'smooth' });
  };

  const deactivateRoom = async (room: CinemaRoomSummary) => {
    if (!window.confirm(`Ngừng hoạt động phòng “${room.name}”?`)) return;
    setError('');
    setMessage('');
    try {
      await api(`/admin/rooms/${room.id}`, { method: 'DELETE' });
      setMessage('Phòng đã được ngừng hoạt động.');
      await load();
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : 'Không thể ngừng hoạt động phòng.',
      );
    }
  };

  return (
    <div className="admin-page">
      <div className="page-title">
        <h1>Rạp, phòng & giá vé</h1>
        <p>Quản lý cấu trúc rạp, sơ đồ ghế và chính sách giá theo từng phòng.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <div className="admin-grid admin-grid-wide cinema-management-grid">
        <div className="admin-stack">
          <form className="panel" onSubmit={submitCinema}>
            <div className="admin-form-title">
              <div>
                <span className="eyebrow">RẠP</span>
                <h3>{editingCinemaId ? 'Chỉnh sửa rạp' : 'Thêm rạp mới'}</h3>
              </div>
              {editingCinemaId && (
                <button
                  className="btn btn-secondary btn-sm"
                  type="button"
                  onClick={() => {
                    setEditingCinemaId(null);
                    setCinemaForm(emptyCinema);
                  }}
                >
                  Hủy sửa
                </button>
              )}
            </div>
            <label>
              Tên rạp
              <input
                value={cinemaForm.name}
                onChange={(event) =>
                  setCinemaForm((value) => ({ ...value, name: event.target.value }))
                }
                required
              />
            </label>
            <label>
              Địa chỉ
              <input
                value={cinemaForm.address}
                onChange={(event) =>
                  setCinemaForm((value) => ({ ...value, address: event.target.value }))
                }
                required
              />
            </label>
            <label className="admin-checkbox-row">
              <input
                type="checkbox"
                checked={cinemaForm.active}
                onChange={(event) =>
                  setCinemaForm((value) => ({ ...value, active: event.target.checked }))
                }
              />
              Rạp đang hoạt động
            </label>
            <button className="btn">{editingCinemaId ? 'Lưu thay đổi' : 'Tạo rạp'}</button>
          </form>

          <form className="panel" onSubmit={submitRoom}>
            <div className="admin-form-title">
              <div>
                <span className="eyebrow">PHÒNG CHIẾU</span>
                <h3>{editingRoomId ? 'Chỉnh sửa phòng' : 'Thêm phòng chiếu'}</h3>
              </div>
              {editingRoomId && (
                <button
                  className="btn btn-secondary btn-sm"
                  type="button"
                  onClick={() => {
                    setEditingRoomId(null);
                    setRoomForm(emptyRoom);
                  }}
                >
                  Hủy sửa
                </button>
              )}
            </div>
            <label>
              Rạp
              <select
                value={selectedCinemaId}
                onChange={(event) => {
                  setSelectedCinemaId(event.target.value);
                  setEditingRoomId(null);
                  setRoomForm(emptyRoom);
                }}
                disabled={Boolean(editingRoomId)}
                required
              >
                <option value="">Chọn rạp</option>
                {cinemas.map((cinema) => (
                  <option key={cinema.id} value={cinema.id}>
                    {cinema.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Tên phòng
              <input
                value={roomForm.name}
                onChange={(event) =>
                  setRoomForm((value) => ({ ...value, name: event.target.value }))
                }
                required
              />
            </label>
            <div className="form-two-col">
              <label>
                Số hàng
                <input
                  type="number"
                  min="6"
                  max="26"
                  value={roomForm.rows}
                  onChange={(event) =>
                    setRoomForm((value) => ({ ...value, rows: Number(event.target.value) }))
                  }
                  required
                />
              </label>
              <label>
                Ghế mỗi hàng
                <input
                  type="number"
                  min="6"
                  max="30"
                  value={roomForm.seatsPerRow}
                  onChange={(event) =>
                    setRoomForm((value) => ({ ...value, seatsPerRow: Number(event.target.value) }))
                  }
                  required
                />
              </label>
            </div>
            <div className="room-price-grid">
              <label>
                Giá ngày thường
                <input
                  type="number"
                  min="0"
                  step="1000"
                  value={roomForm.weekdayBasePrice}
                  onChange={(event) =>
                    setRoomForm((value) => ({
                      ...value,
                      weekdayBasePrice: Number(event.target.value),
                    }))
                  }
                  required
                />
              </label>
              <label>
                Giá cuối tuần
                <input
                  type="number"
                  min="0"
                  step="1000"
                  value={roomForm.weekendBasePrice}
                  onChange={(event) =>
                    setRoomForm((value) => ({
                      ...value,
                      weekendBasePrice: Number(event.target.value),
                    }))
                  }
                  required
                />
              </label>
              <label>
                Phụ thu VIP
                <input
                  type="number"
                  min="0"
                  step="1000"
                  value={roomForm.vipSurcharge}
                  onChange={(event) =>
                    setRoomForm((value) => ({ ...value, vipSurcharge: Number(event.target.value) }))
                  }
                  required
                />
              </label>
            </div>
            <label className="admin-checkbox-row">
              <input
                type="checkbox"
                checked={roomForm.active}
                onChange={(event) =>
                  setRoomForm((value) => ({ ...value, active: event.target.checked }))
                }
              />
              Phòng đang hoạt động
            </label>
            {editingRoomId && (
              <p className="muted admin-form-note">
                Nếu phòng đã có suất chiếu, hệ thống khóa thay đổi số hàng/ghế để bảo toàn lịch sử;
                bạn vẫn sửa được tên, trạng thái và giá.
              </p>
            )}
            <button className="btn" disabled={!selectedCinemaId || !selectedCinema?.active}>
              {editingRoomId ? 'Lưu phòng' : 'Tạo phòng & ghế'}
            </button>
          </form>
        </div>

        <section className="panel cinema-admin-list-panel">
          <div className="section-head compact">
            <div>
              <h2>Danh sách rạp</h2>
              <p>{cinemas.length} rạp · có thể xem và quản lý phòng ngay tại đây</p>
            </div>
          </div>
          <div className="cinema-admin-list">
            {cinemas.map((cinema) => (
              <article
                className={`cinema-admin-card ${selectedCinema?.id === cinema.id ? 'selected' : ''} ${!cinema.active ? 'is-inactive' : ''}`}
                key={cinema.id}
              >
                <div className="cinema-admin-card-head">
                  <button
                    className="cinema-admin-select"
                    type="button"
                    onClick={() => setSelectedCinemaId(String(cinema.id))}
                  >
                    <span className={`status-dot ${cinema.active ? 'online' : 'offline'}`} />
                    <div>
                      <h3>{cinema.name}</h3>
                      <p>{cinema.address}</p>
                    </div>
                  </button>
                  <div className="admin-inline-actions">
                    <button
                      className="btn btn-secondary btn-sm"
                      type="button"
                      onClick={() => editCinema(cinema)}
                    >
                      Sửa rạp
                    </button>
                    {cinema.active && (
                      <button
                        className="btn btn-danger btn-sm"
                        type="button"
                        onClick={() => void deactivateCinema(cinema)}
                      >
                        Xóa / ngừng
                      </button>
                    )}
                  </div>
                </div>

                <div className="cinema-admin-summary">
                  <span>{cinema.rooms.length} phòng</span>
                  <span>{cinema.rooms.reduce((total, room) => total + room.seatCount, 0)} ghế</span>
                  <span>
                    {cinema.rooms.reduce((total, room) => total + room.vipSeatCount, 0)} VIP
                  </span>
                </div>

                <div className="admin-room-list">
                  {cinema.rooms.map((room) => (
                    <div
                      className={`admin-room-card ${!room.active ? 'is-inactive' : ''}`}
                      key={room.id}
                    >
                      <div className="admin-room-main">
                        <div>
                          <strong>{room.name}</strong>
                          <p>
                            {room.rows} hàng × {room.seatsPerRow} ghế · {room.vipSeatCount} VIP
                          </p>
                        </div>
                        <span
                          className={room.active ? 'room-status-active' : 'room-status-inactive'}
                        >
                          {room.active ? 'Hoạt động' : 'Ngừng'}
                        </span>
                      </div>
                      <div className="admin-room-prices">
                        <span>
                          <small>Ngày thường</small>
                          <strong>{money(room.weekdayBasePrice)}</strong>
                        </span>
                        <span>
                          <small>Cuối tuần</small>
                          <strong>{money(room.weekendBasePrice)}</strong>
                        </span>
                        <span>
                          <small>VIP cộng</small>
                          <strong>+{money(room.vipSurcharge)}</strong>
                        </span>
                      </div>
                      <div className="admin-inline-actions room-actions">
                        <button
                          className="btn btn-secondary btn-sm"
                          type="button"
                          onClick={() => editRoom(cinema, room)}
                        >
                          Sửa phòng
                        </button>
                        {room.active && (
                          <button
                            className="btn btn-danger btn-sm"
                            type="button"
                            onClick={() => void deactivateRoom(room)}
                          >
                            Xóa / ngừng
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                  {cinema.rooms.length === 0 && (
                    <div className="empty">Rạp chưa có phòng chiếu.</div>
                  )}
                </div>
              </article>
            ))}
            {cinemas.length === 0 && <div className="empty">Chưa có rạp.</div>}
          </div>
        </section>
      </div>
    </div>
  );
}
