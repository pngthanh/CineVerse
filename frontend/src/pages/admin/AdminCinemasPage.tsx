import { FormEvent, useEffect, useMemo, useState } from 'react';
import { ApiError, api } from '../../lib/api';
import type { Cinema } from '../../types';
export function AdminCinemasPage() {
    const [cinemas, setCinemas] = useState<Cinema[]>([]);
    const [cinemaName, setCinemaName] = useState('');
    const [address, setAddress] = useState('');
    const [selectedCinemaId, setSelectedCinemaId] = useState('');
    const [roomName, setRoomName] = useState('');
    const [rows, setRows] = useState(8);
    const [seatsPerRow, setSeatsPerRow] = useState(10);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const load = async () => {
        const data = await api<Cinema[]>('/cinemas');
        setCinemas(data);
        if (!selectedCinemaId && data[0]) setSelectedCinemaId(String(data[0].id));
    };

    useEffect(() => {
        let active = true;
        void api<Cinema[]>('/cinemas')
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
    const selectedCinema = useMemo(() => cinemas.find((cinema) => cinema.id === Number(selectedCinemaId)), [cinemas, selectedCinemaId]);
    const createCinema = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        setMessage('');
        try {
            await api('/admin/cinemas', {
                method: 'POST',
                body: JSON.stringify({ name: cinemaName, address, active: true }),
            });
            setCinemaName('');
            setAddress('');
            setMessage('Đã tạo rạp mới.');
            await load();
        }
        catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể tạo rạp.');
        }
    };
    const createRoom = async (event: FormEvent) => {
        event.preventDefault();
        if (!selectedCinemaId)
            return;
        setError('');
        setMessage('');
        try {
            await api(`/admin/cinemas/${selectedCinemaId}/rooms`, {
                method: 'POST',
                body: JSON.stringify({ name: roomName, rows, seatsPerRow }),
            });
            setRoomName('');
            setMessage('Đã tạo phòng và sơ đồ ghế tự động.');
            await load();
        }
        catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể tạo phòng.');
        }
    };
    return (<div className="admin-page">
      <div className="page-title">
        <h1>Rạp & phòng chiếu</h1>
        <p>Quản lý cấu trúc Rạp → Phòng → Ghế.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <div className="admin-grid admin-grid-wide">
        <div className="admin-stack">
          <form className="panel" onSubmit={createCinema}>
            <h3>Thêm rạp</h3>
            <label>
              Tên rạp
              <input value={cinemaName} onChange={(event) => setCinemaName(event.target.value)} required/>
            </label>
            <label>
              Địa chỉ
              <input value={address} onChange={(event) => setAddress(event.target.value)} required/>
            </label>
            <button className="btn">Tạo rạp</button>
          </form>

          <form className="panel" onSubmit={createRoom}>
            <h3>Thêm phòng chiếu</h3>
            <label>
              Rạp
              <select value={selectedCinemaId} onChange={(event) => setSelectedCinemaId(event.target.value)} required>
                <option value="">Chọn rạp</option>
                {cinemas.map((cinema) => (<option key={cinema.id} value={cinema.id}>{cinema.name}</option>))}
              </select>
            </label>
            <label>
              Tên phòng
              <input value={roomName} onChange={(event) => setRoomName(event.target.value)} required/>
            </label>
            <div className="form-two-col">
              <label>
                Số hàng
                <input type="number" min="1" max="20" value={rows} onChange={(event) => setRows(Number(event.target.value))} required/>
              </label>
              <label>
                Ghế mỗi hàng
                <input type="number" min="2" max="30" value={seatsPerRow} onChange={(event) => setSeatsPerRow(Number(event.target.value))} required/>
              </label>
            </div>
            <p className="muted">Khu ghế VIP nằm ở trung tâm phòng; chừa 2 hàng và 2 cột ngoài cùng làm ghế thường.</p>
            <button className="btn">Tạo phòng & ghế</button>
          </form>
        </div>

        <section className="panel">
          <div className="section-head compact">
            <div>
              <h2>Danh sách rạp</h2>
              <p>{cinemas.length} rạp đang hoạt động</p>
            </div>
          </div>
          {cinemas.map((cinema) => (<div className={`admin-cinema-card ${selectedCinema?.id === cinema.id ? 'selected' : ''}`} key={cinema.id} onClick={() => setSelectedCinemaId(String(cinema.id))}>
              <div className="admin-list-item no-border">
                <div>
                  <h3>{cinema.name}</h3>
                  <p>{cinema.address}</p>
                </div>
                <span>{cinema.rooms.length} phòng</span>
              </div>
              <div className="room-chip-list">
                {cinema.rooms.map((room) => (<span className="room-chip" key={room.id}>
                    {room.name} · {room.seatCount} ghế
                  </span>))}
                {cinema.rooms.length === 0 && <span className="muted">Chưa có phòng.</span>}
              </div>
            </div>))}
        </section>
      </div>
    </div>);
}

