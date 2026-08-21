import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Modal } from '../../components/Modal';
import { ApiError, api } from '../../lib/api';
import { money } from '../../lib/format';
import { loadVietnamAddresses, type VietnamProvince } from '../../lib/vietnamAddress';
import type { Cinema, CinemaRoomSummary } from '../../types';

type CinemaForm = { name: string; addressDetail: string; active: boolean };
type ClosureTarget = { kind: 'cinema' | 'room'; id: number; name: string } | null;
type RoomForm = {
    name: string;
    rows: number;
    seatsPerRow: number;
    weekdayBasePrice: number;
    weekendBasePrice: number;
    vipSurcharge: number;
    active: boolean;
};

const emptyCinema: CinemaForm = { name: '', addressDetail: '', active: true };
const emptyRoom: RoomForm = {
    name: '', rows: 8, seatsPerRow: 10,
    weekdayBasePrice: 70000, weekendBasePrice: 100000,
    vipSurcharge: 20000, active: true,
};

function roomToForm(room: CinemaRoomSummary): RoomForm {
    return {
        name: room.name, rows: room.rows, seatsPerRow: room.seatsPerRow,
        weekdayBasePrice: room.weekdayBasePrice,
        weekendBasePrice: room.weekendBasePrice,
        vipSurcharge: room.vipSurcharge, active: room.active,
    };
}

export function AdminCinemasPage() {
    const [cinemas, setCinemas] = useState<Cinema[]>([]);
    const [selectedCinemaId, setSelectedCinemaId] = useState('');
    const [cinemaForm, setCinemaForm] = useState<CinemaForm>(emptyCinema);
    const [editingCinemaId, setEditingCinemaId] = useState<number | null>(null);
    const [roomForm, setRoomForm] = useState<RoomForm>(emptyRoom);
    const [editingRoomId, setEditingRoomId] = useState<number | null>(null);
    const [cinemaModalOpen, setCinemaModalOpen] = useState(false);
    const [roomModalOpen, setRoomModalOpen] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [provinces, setProvinces] = useState<VietnamProvince[]>([]);
    const [provinceCode, setProvinceCode] = useState('');
    const [districtCode, setDistrictCode] = useState('');
    const [wardCode, setWardCode] = useState('');
    const [closureTarget, setClosureTarget] = useState<ClosureTarget>(null);
    const [closureAt, setClosureAt] = useState('');
    const [closureReason, setClosureReason] = useState('Bảo trì / ngừng vận hành theo kế hoạch.');

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
        api<Cinema[]>('/admin/cinemas').then((data) => {
            if (!active) return;
            setCinemas(data);
            if (data[0]) setSelectedCinemaId(String(data[0].id));
        }).catch(() => { if (active) setError('Không thể tải danh sách rạp.'); });
        return () => { active = false; };
    }, []);

    const selectedCinema = useMemo(
        () => cinemas.find((cinema) => cinema.id === Number(selectedCinemaId)),
        [cinemas, selectedCinemaId],
    );

    useEffect(() => {
        void loadVietnamAddresses().then(setProvinces).catch(() => setProvinces([]));
    }, []);

    const province = useMemo(() => provinces.find((item) => String(item.code) === provinceCode), [provinces, provinceCode]);
    const district = useMemo(() => province?.districts.find((item) => String(item.code) === districtCode), [province, districtCode]);
    const ward = useMemo(() => district?.wards.find((item) => String(item.code) === wardCode), [district, wardCode]);

    const resetAddressSelection = () => {
        setProvinceCode(''); setDistrictCode(''); setWardCode('');
    };

    const openCreateCinema = () => {
        setEditingCinemaId(null); setCinemaForm(emptyCinema); resetAddressSelection(); setError(''); setCinemaModalOpen(true);
    };
    const openCreateRoom = () => {
        if (!selectedCinemaId) { setError('Hãy chọn một rạp trước khi tạo phòng.'); return; }
        setEditingRoomId(null); setRoomForm(emptyRoom); setError(''); setRoomModalOpen(true);
    };
    const editCinema = (cinema: Cinema) => {
        setEditingCinemaId(cinema.id);
        setCinemaForm({ name: cinema.name, addressDetail: cinema.address, active: cinema.active });
        resetAddressSelection();
        setCinemaModalOpen(true); setError('');
    };
    const editRoom = (cinema: Cinema, room: CinemaRoomSummary) => {
        setSelectedCinemaId(String(cinema.id)); setEditingRoomId(room.id);
        setRoomForm(roomToForm(room)); setRoomModalOpen(true); setError('');
    };

    const submitCinema = async (event: FormEvent) => {
        event.preventDefault(); setError(''); setMessage('');
        try {
            const selectedAddress = province && district && ward;
            if (!editingCinemaId && !selectedAddress) {
                setError('Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện và Phường/Xã.');
                return;
            }
            const address = selectedAddress
                ? [cinemaForm.addressDetail.trim(), ward.name, district.name, province.name].filter(Boolean).join(', ')
                : cinemaForm.addressDetail.trim();
            if (!address) { setError('Vui lòng nhập địa chỉ rạp.'); return; }
            const payload = { name: cinemaForm.name, address, active: cinemaForm.active };
            if (editingCinemaId) {
                await api(`/admin/cinemas/${editingCinemaId}`, { method: 'PUT', body: JSON.stringify(payload) });
                setMessage('Đã cập nhật rạp.');
            } else {
                await api('/admin/cinemas', { method: 'POST', body: JSON.stringify(payload) });
                setMessage('Đã tạo rạp mới.');
            }
            setCinemaModalOpen(false); setEditingCinemaId(null); setCinemaForm(emptyCinema); await load();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể lưu rạp.');
        }
    };

    const submitRoom = async (event: FormEvent) => {
        event.preventDefault();
        if (!selectedCinemaId) return;
        setError(''); setMessage('');
        try {
            if (editingRoomId) {
                await api(`/admin/rooms/${editingRoomId}`, { method: 'PUT', body: JSON.stringify(roomForm) });
                setMessage('Đã cập nhật phòng chiếu.');
            } else {
                await api(`/admin/cinemas/${selectedCinemaId}/rooms`, { method: 'POST', body: JSON.stringify(roomForm) });
                setMessage('Đã tạo phòng và sơ đồ ghế tự động.');
            }
            setRoomModalOpen(false); setEditingRoomId(null); setRoomForm(emptyRoom); await load();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể lưu phòng chiếu.');
        }
    };

    const deactivateCinema = async (cinema: Cinema) => {
        if (!window.confirm(`Ngừng hoạt động rạp “${cinema.name}”?`)) return;
        setError(''); setMessage('');
        try {
            await api(`/admin/cinemas/${cinema.id}`, { method: 'DELETE' });
            setMessage('Rạp đã được ngừng hoạt động. Dữ liệu lịch sử vẫn được giữ nguyên.'); await load();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể ngừng hoạt động rạp.');
        }
    };

    const deactivateRoom = async (room: CinemaRoomSummary) => {
        if (!window.confirm(`Ngừng hoạt động phòng “${room.name}”?`)) return;
        setError(''); setMessage('');
        try {
            await api(`/admin/rooms/${room.id}`, { method: 'DELETE' });
            setMessage('Phòng đã được ngừng hoạt động.'); await load();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể ngừng hoạt động phòng.');
        }
    };

    const openClosure = (target: ClosureTarget) => {
        if (!target) return;
        const future = new Date(Date.now() + 60 * 60 * 1000);
        const local = new Date(future.getTime() - future.getTimezoneOffset() * 60_000)
            .toISOString().slice(0, 16);
        setClosureTarget(target);
        setClosureAt(local);
        setClosureReason('Bảo trì / ngừng vận hành theo kế hoạch.');
        setError('');
    };

    const submitClosure = async (event: FormEvent) => {
        event.preventDefault();
        if (!closureTarget) return;
        setError(''); setMessage('');
        try {
            const path = closureTarget.kind === 'cinema'
                ? `/admin/cinemas/${closureTarget.id}/closure`
                : `/admin/rooms/${closureTarget.id}/closure`;
            await api(path, {
                method: 'PATCH',
                body: JSON.stringify({ closesAt: closureAt, reason: closureReason }),
            });
            setMessage(`Đã lập lịch ngừng hoạt động cho ${closureTarget.name}. Các suất từ mốc này trở đi sẽ bị hủy và booking đã thanh toán chuyển sang chờ hoàn tiền.`);
            setClosureTarget(null);
            await load();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể lập lịch ngừng hoạt động.');
        }
    };

    return <div className="admin-page">
        <div className="page-title admin-page-title-actions">
            <div><h1>Rạp, phòng & giá vé</h1><p>Quản lý cấu trúc rạp, sơ đồ ghế và chính sách giá theo từng phòng.</p></div>
            <div className="admin-title-buttons"><button className="btn btn-secondary" type="button" onClick={openCreateRoom} disabled={!selectedCinema?.active}>+ Thêm phòng</button><button className="btn" type="button" onClick={openCreateCinema}>+ Thêm rạp</button></div>
        </div>
        {error && !cinemaModalOpen && !roomModalOpen && <div className="alert alert-error">{error}</div>}
        {message && <div className="alert alert-success">{message}</div>}

        <section className="panel cinema-admin-list-panel admin-full-panel">
            <div className="section-head compact"><div><h2>Danh sách rạp</h2><p>{cinemas.length} rạp · chọn rạp để thêm phòng mới</p></div></div>
            <div className="cinema-admin-list">{cinemas.map((cinema) => <article className={`cinema-admin-card ${selectedCinema?.id === cinema.id ? 'selected' : ''} ${!cinema.active ? 'is-inactive' : ''}`} key={cinema.id}>
                <div className="cinema-admin-card-head">
                    <button className="cinema-admin-select" type="button" onClick={() => setSelectedCinemaId(String(cinema.id))}><span className={`status-dot ${cinema.active ? 'online' : 'offline'}`} /><div><h3>{cinema.name}</h3><p>{cinema.address}</p></div></button>
                    <div className="admin-inline-actions"><button className="btn btn-secondary btn-sm" type="button" onClick={() => editCinema(cinema)}>Sửa rạp</button>{cinema.active && <button className="btn btn-secondary btn-sm" type="button" onClick={() => openClosure({ kind: 'cinema', id: cinema.id, name: cinema.name })}>Lập lịch đóng</button>}{cinema.active && <button className="btn btn-danger btn-sm" type="button" onClick={() => void deactivateCinema(cinema)}>Ngừng ngay</button>}</div>
                </div>
                <div className="cinema-admin-summary"><span>{cinema.rooms.length} phòng</span><span>{cinema.rooms.reduce((total, room) => total + room.seatCount, 0)} ghế</span><span>{cinema.rooms.reduce((total, room) => total + room.vipSeatCount, 0)} VIP</span>{cinema.closesAt && <span>Đóng từ {new Date(cinema.closesAt).toLocaleString('vi-VN')}</span>}</div>{cinema.closureReason && <p className="muted lifecycle-reason">{cinema.closureReason}</p>}
                <div className="admin-room-list">{cinema.rooms.map((room) => <div className={`admin-room-card ${!room.active ? 'is-inactive' : ''}`} key={room.id}>
                    <div className="admin-room-main"><div><strong>{room.name}</strong><p>{room.rows} hàng × {room.seatsPerRow} ghế · {room.vipSeatCount} VIP</p></div><span className={room.active ? 'room-status-active' : 'room-status-inactive'}>{room.active ? 'Hoạt động' : 'Ngừng'}</span></div>
                    {room.closesAt && <p className="muted lifecycle-reason">Đóng từ {new Date(room.closesAt).toLocaleString('vi-VN')}{room.closureReason ? ` · ${room.closureReason}` : ''}</p>}<div className="admin-room-prices"><span><small>Ngày thường</small><strong>{money(room.weekdayBasePrice)}</strong></span><span><small>Cuối tuần</small><strong>{money(room.weekendBasePrice)}</strong></span><span><small>VIP cộng</small><strong>+{money(room.vipSurcharge)}</strong></span></div>
                    <div className="admin-inline-actions room-actions"><button className="btn btn-secondary btn-sm" type="button" onClick={() => editRoom(cinema, room)}>Sửa phòng</button>{room.active && <button className="btn btn-secondary btn-sm" type="button" onClick={() => openClosure({ kind: 'room', id: room.id, name: room.name })}>Lập lịch đóng</button>}{room.active && <button className="btn btn-danger btn-sm" type="button" onClick={() => void deactivateRoom(room)}>Ngừng ngay</button>}</div>
                </div>)}{cinema.rooms.length === 0 && <div className="empty">Rạp chưa có phòng chiếu.</div>}</div>
            </article>)}{cinemas.length === 0 && <div className="empty">Chưa có rạp.</div>}</div>
        </section>

        <Modal open={cinemaModalOpen} title={editingCinemaId ? 'Chỉnh sửa rạp' : 'Thêm rạp'} onClose={() => setCinemaModalOpen(false)}>
            {error && <div className="alert alert-error">{error}</div>}
            <form className="modal-form admin-clean-form" onSubmit={submitCinema}>
                <label>Tên rạp<input value={cinemaForm.name} onChange={(e) => setCinemaForm((value) => ({ ...value, name: e.target.value }))} required /></label>
                <div className="form-three-col address-grid">
                    <label>Tỉnh / Thành<select value={provinceCode} onChange={(e) => { setProvinceCode(e.target.value); setDistrictCode(''); setWardCode(''); }} required={!editingCinemaId}><option value="">Chọn tỉnh / thành</option>{provinces.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
                    <label>Quận / Huyện<select value={districtCode} disabled={!province} onChange={(e) => { setDistrictCode(e.target.value); setWardCode(''); }} required={!editingCinemaId}><option value="">Chọn quận / huyện</option>{province?.districts.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
                    <label>Phường / Xã<select value={wardCode} disabled={!district} onChange={(e) => setWardCode(e.target.value)} required={!editingCinemaId}><option value="">Chọn phường / xã</option>{district?.wards.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
                </div>
                <label>Địa chỉ chi tiết<input value={cinemaForm.addressDetail} onChange={(e) => setCinemaForm((value) => ({ ...value, addressDetail: e.target.value }))} placeholder={editingCinemaId ? 'Địa chỉ hiện tại — có thể sửa trực tiếp hoặc chọn lại khu vực phía trên' : 'Số nhà, tên đường, hẻm...'} required /></label>
                {editingCinemaId && <small className="muted">Khi sửa rạp cũ, nếu không chọn lại Tỉnh/Huyện/Xã thì hệ thống giữ nội dung địa chỉ trong ô chi tiết.</small>}
                <label className="admin-checkbox-row"><input type="checkbox" checked={cinemaForm.active} onChange={(e) => setCinemaForm((value) => ({ ...value, active: e.target.checked }))} />Rạp đang hoạt động</label>
                <div className="modal-actions"><button className="btn btn-secondary" type="button" onClick={() => setCinemaModalOpen(false)}>Hủy</button><button className="btn">{editingCinemaId ? 'Lưu thay đổi' : 'Tạo rạp'}</button></div>
            </form>
        </Modal>

        <Modal open={roomModalOpen} title={editingRoomId ? 'Chỉnh sửa phòng chiếu' : `Thêm phòng · ${selectedCinema?.name ?? ''}`} onClose={() => setRoomModalOpen(false)} wide>
            {error && <div className="alert alert-error">{error}</div>}
            <form className="modal-form admin-clean-form" onSubmit={submitRoom}><label>Rạp<select value={selectedCinemaId} onChange={(e) => setSelectedCinemaId(e.target.value)} disabled={Boolean(editingRoomId)} required>{cinemas.filter((cinema) => cinema.active).map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}</select></label><label>Tên phòng<input value={roomForm.name} onChange={(e) => setRoomForm((value) => ({ ...value, name: e.target.value }))} required /></label><div className="form-two-col"><label>Số hàng<input type="number" min="6" max="26" value={roomForm.rows} onChange={(e) => setRoomForm((value) => ({ ...value, rows: Number(e.target.value) }))} required /></label><label>Ghế mỗi hàng<input type="number" min="6" max="30" value={roomForm.seatsPerRow} onChange={(e) => setRoomForm((value) => ({ ...value, seatsPerRow: Number(e.target.value) }))} required /></label></div><div className="form-three-col"><label>Giá ngày thường<input type="number" min="0" step="1000" value={roomForm.weekdayBasePrice} onChange={(e) => setRoomForm((value) => ({ ...value, weekdayBasePrice: Number(e.target.value) }))} required /></label><label>Giá cuối tuần<input type="number" min="0" step="1000" value={roomForm.weekendBasePrice} onChange={(e) => setRoomForm((value) => ({ ...value, weekendBasePrice: Number(e.target.value) }))} required /></label><label>Phụ thu VIP<input type="number" min="0" step="1000" value={roomForm.vipSurcharge} onChange={(e) => setRoomForm((value) => ({ ...value, vipSurcharge: Number(e.target.value) }))} required /></label></div><label className="admin-checkbox-row"><input type="checkbox" checked={roomForm.active} onChange={(e) => setRoomForm((value) => ({ ...value, active: e.target.checked }))} />Phòng đang hoạt động</label>{editingRoomId && <p className="muted admin-form-note">Nếu phòng đã có suất chiếu, hệ thống khóa thay đổi số hàng/ghế để bảo toàn lịch sử; bạn vẫn sửa được tên, trạng thái và giá.</p>}<div className="modal-actions"><button className="btn btn-secondary" type="button" onClick={() => setRoomModalOpen(false)}>Hủy</button><button className="btn" disabled={!selectedCinemaId || !selectedCinema?.active}>{editingRoomId ? 'Lưu phòng' : 'Tạo phòng & ghế'}</button></div></form>
        </Modal>

        <Modal open={Boolean(closureTarget)} title={`Lập lịch ngừng hoạt động · ${closureTarget?.name ?? ''}`} onClose={() => setClosureTarget(null)}>
            {error && <div className="alert alert-error">{error}</div>}
            <form className="modal-form admin-clean-form" onSubmit={submitClosure}>
                <label>Thời điểm ngừng hoạt động<input type="datetime-local" value={closureAt} onChange={(e) => setClosureAt(e.target.value)} required /></label>
                <label>Lý do<textarea value={closureReason} onChange={(e) => setClosureReason(e.target.value)} required /></label>
                <p className="muted admin-form-note">Các suất trước thời điểm này vẫn hoạt động bình thường. Suất từ thời điểm này trở đi sẽ bị hủy; booking đã thanh toán chuyển sang Chờ hoàn tiền để G10 xử lý hoàn tiền VNPAY.</p>
                <div className="modal-actions"><button className="btn btn-secondary" type="button" onClick={() => setClosureTarget(null)}>Hủy</button><button className="btn btn-danger">Xác nhận lịch đóng</button></div>
            </form>
        </Modal>
    </div>;
}
