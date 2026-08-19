import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Modal } from '../../components/Modal';
import { ApiError, api } from '../../lib/api';
import { money } from '../../lib/format';
import type { Movie, UserProfile, Voucher } from '../../types';

type VoucherForm = {
    code: string;
    title: string;
    description: string;
    discountType: 'PERCENT' | 'FIXED';
    discountValue: number;
    minOrderAmount: number;
    maxDiscountAmount: number | '';
    startsAt: string;
    expiresAt: string;
    active: boolean;
    publicVisible: boolean;
    audience: 'ALL' | 'SELECTED_USERS';
    movieId: number | '';
    usageLimit: number | '';
    perUserLimit: number | '';
    assignedUserIds: number[];
};

const localDateTime = (date: Date) => {
    const copy = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return copy.toISOString().slice(0, 16);
};

const emptyForm = (): VoucherForm => ({
    code: '', title: '', description: '', discountType: 'PERCENT', discountValue: 10,
    minOrderAmount: 0, maxDiscountAmount: '', startsAt: localDateTime(new Date()),
    expiresAt: localDateTime(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)),
    active: true, publicVisible: true, audience: 'ALL', movieId: '', usageLimit: '',
    perUserLimit: 1, assignedUserIds: [],
});

export function AdminVouchersPage() {
    const [vouchers, setVouchers] = useState<Voucher[]>([]);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [users, setUsers] = useState<UserProfile[]>([]);
    const [form, setForm] = useState<VoucherForm>(emptyForm);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState('');
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const load = async () => {
        const [voucherData, movieData, userData] = await Promise.all([
            api<Voucher[]>('/admin/vouchers'), api<Movie[]>('/admin/movies'), api<UserProfile[]>('/admin/users'),
        ]);
        setVouchers(voucherData); setMovies(movieData); setUsers(userData);
    };

    useEffect(() => {
        let active = true;
        void Promise.all([api<Voucher[]>('/admin/vouchers'), api<Movie[]>('/admin/movies'), api<UserProfile[]>('/admin/users')])
            .then(([voucherData, movieData, userData]) => { if (active) { setVouchers(voucherData); setMovies(movieData); setUsers(userData); } })
            .catch(() => { if (active) setError('Không thể tải dữ liệu voucher.'); });
        return () => { active = false; };
    }, []);

    const filtered = useMemo(() => {
        const keyword = query.trim().toLowerCase();
        if (!keyword) return vouchers;
        return vouchers.filter((voucher) => [voucher.code, voucher.title, voucher.movieTitle ?? ''].some((value) => value.toLowerCase().includes(keyword)));
    }, [vouchers, query]);

    const startCreate = () => { setEditingId(null); setForm(emptyForm()); setError(''); setMessage(''); setOpen(true); };
    const startEdit = (voucher: Voucher) => {
        setEditingId(voucher.id);
        setForm({ code: voucher.code, title: voucher.title, description: voucher.description ?? '', discountType: voucher.discountType, discountValue: voucher.discountValue, minOrderAmount: voucher.minOrderAmount, maxDiscountAmount: voucher.maxDiscountAmount ?? '', startsAt: voucher.startsAt.slice(0, 16), expiresAt: voucher.expiresAt.slice(0, 16), active: voucher.active, publicVisible: voucher.publicVisible, audience: voucher.audience, movieId: voucher.movieId ?? '', usageLimit: voucher.usageLimit ?? '', perUserLimit: voucher.perUserLimit ?? '', assignedUserIds: voucher.assignedUserIds ?? [] });
        setError(''); setMessage(''); setOpen(true);
    };
    const update = <K extends keyof VoucherForm>(key: K, value: VoucherForm[K]) => setForm((current) => ({ ...current, [key]: value }));
    const toggleUser = (userId: number) => update('assignedUserIds', form.assignedUserIds.includes(userId) ? form.assignedUserIds.filter((id) => id !== userId) : [...form.assignedUserIds, userId]);

    const submit = async (event: FormEvent) => {
        event.preventDefault(); setError(''); setMessage('');
        try {
            await api(editingId ? `/admin/vouchers/${editingId}` : '/admin/vouchers', { method: editingId ? 'PUT' : 'POST', body: JSON.stringify({ ...form, code: form.code.toUpperCase(), maxDiscountAmount: form.maxDiscountAmount === '' ? null : form.maxDiscountAmount, movieId: form.movieId === '' ? null : form.movieId, usageLimit: form.usageLimit === '' ? null : form.usageLimit, perUserLimit: form.perUserLimit === '' ? null : form.perUserLimit, assignedUserIds: form.audience === 'SELECTED_USERS' ? form.assignedUserIds : [] }) });
            setMessage(editingId ? 'Đã cập nhật voucher.' : 'Đã tạo voucher.'); setOpen(false); await load();
        } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể lưu voucher.'); }
    };

    const deactivate = async (voucher: Voucher) => {
        if (!window.confirm(`Vô hiệu hóa mã ${voucher.code}?`)) return;
        try { await api(`/admin/vouchers/${voucher.id}`, { method: 'DELETE' }); await load(); }
        catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể vô hiệu hóa voucher.'); }
    };

    return <div className="admin-page">
        <div className="page-title admin-page-title-row"><div><h1>Quản lý voucher</h1><p>Tạo ưu đãi công khai hoặc cấp riêng cho từng tài khoản, kèm điều kiện áp dụng.</p></div><button className="btn" type="button" onClick={startCreate}>+ Thêm voucher</button></div>
        {error && <div className="alert alert-error">{error}</div>}{message && <div className="alert alert-success">{message}</div>}
        <section className="panel admin-voucher-toolbar"><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm mã, tên ưu đãi hoặc phim..." /><span>{filtered.length} voucher</span></section>
        <section className="panel table-wrap"><table><thead><tr><th>Mã / ưu đãi</th><th>Điều kiện</th><th>Đối tượng</th><th>Hiệu lực</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{filtered.map((voucher) => <tr key={voucher.id}><td><div className="voucher-admin-code"><code>{voucher.code}</code><strong>{voucher.title}</strong><small>{voucher.discountType === 'PERCENT' ? `Giảm ${voucher.discountValue}%` : `Giảm ${money(voucher.discountValue)}`}</small></div></td><td><small>Đơn từ {money(voucher.minOrderAmount)}</small><br/><small>{voucher.movieTitle ? `Phim: ${voucher.movieTitle}` : 'Mọi phim'}</small></td><td>{voucher.audience === 'ALL' ? 'Mọi người' : `${voucher.assignedUserIds.length} tài khoản`}<br/><small>{voucher.publicVisible ? 'Hiện ở trang chủ' : 'Không quảng bá'}</small></td><td><small>{new Date(voucher.startsAt).toLocaleString('vi-VN')}</small><br/><small>→ {new Date(voucher.expiresAt).toLocaleString('vi-VN')}</small></td><td><span className={`status-pill ${voucher.active ? 'success' : 'muted'}`}>{voucher.active ? 'Hoạt động' : 'Đã tắt'}</span></td><td><div className="row table-actions"><button className="btn btn-sm btn-secondary" type="button" onClick={() => startEdit(voucher)}>Sửa</button><button className="btn btn-sm btn-danger" type="button" disabled={!voucher.active} onClick={() => void deactivate(voucher)}>Vô hiệu hóa</button></div></td></tr>)}</tbody></table></section>

        <Modal open={open} title={editingId ? 'Chỉnh sửa voucher' : 'Thêm voucher'} wide onClose={() => setOpen(false)}>
            <form className="voucher-admin-form admin-clean-form" onSubmit={submit}>
                <div className="form-two-col"><label>Mã voucher<input value={form.code} onChange={(e) => update('code', e.target.value.toUpperCase())} maxLength={30} required /></label><label>Tên ưu đãi<input value={form.title} onChange={(e) => update('title', e.target.value)} required /></label></div>
                <label>Mô tả<textarea value={form.description} onChange={(e) => update('description', e.target.value)} /></label>
                <div className="form-three-col"><label>Kiểu giảm<select value={form.discountType} onChange={(e) => update('discountType', e.target.value as VoucherForm['discountType'])}><option value="PERCENT">Phần trăm</option><option value="FIXED">Số tiền cố định</option></select></label><label>{form.discountType === 'PERCENT' ? 'Mức giảm (%)' : 'Số tiền giảm'}<input type="number" min="0" max={form.discountType === 'PERCENT' ? 100 : undefined} value={form.discountValue} onChange={(e) => update('discountValue', Number(e.target.value))} required /></label><label>Giảm tối đa<input type="number" min="0" value={form.maxDiscountAmount} onChange={(e) => update('maxDiscountAmount', e.target.value === '' ? '' : Number(e.target.value))} placeholder="Không giới hạn" /></label></div>
                <div className="form-three-col"><label>Đơn tối thiểu<input type="number" min="0" value={form.minOrderAmount} onChange={(e) => update('minOrderAmount', Number(e.target.value))} /></label><label>Tổng lượt dùng<input type="number" min="1" value={form.usageLimit} onChange={(e) => update('usageLimit', e.target.value === '' ? '' : Number(e.target.value))} placeholder="Không giới hạn" /></label><label>Lượt / tài khoản<input type="number" min="1" value={form.perUserLimit} onChange={(e) => update('perUserLimit', e.target.value === '' ? '' : Number(e.target.value))} placeholder="Không giới hạn" /></label></div>
                <div className="form-two-col"><label>Bắt đầu<input type="datetime-local" value={form.startsAt} onChange={(e) => update('startsAt', e.target.value)} required /></label><label>Hết hạn<input type="datetime-local" value={form.expiresAt} onChange={(e) => update('expiresAt', e.target.value)} required /></label></div>
                <div className="form-two-col"><label>Phim áp dụng<select value={form.movieId} onChange={(e) => update('movieId', e.target.value === '' ? '' : Number(e.target.value))}><option value="">Tất cả phim</option>{movies.map((movie) => <option key={movie.id} value={movie.id}>{movie.title}</option>)}</select></label><label>Đối tượng<select value={form.audience} onChange={(e) => update('audience', e.target.value as VoucherForm['audience'])}><option value="ALL">Mọi người</option><option value="SELECTED_USERS">Tài khoản được chọn</option></select></label></div>
                {form.audience === 'SELECTED_USERS' && <div className="voucher-user-picker"><strong>Chọn tài khoản được dùng mã</strong><div>{users.filter((user) => user.role === 'CUSTOMER').map((user) => <label key={user.id}><input type="checkbox" checked={form.assignedUserIds.includes(user.id)} onChange={() => toggleUser(user.id)} /><span><b>{user.fullName}</b><small>{user.username || user.email || user.googleEmail || `#${user.id}`}</small></span></label>)}</div></div>}
                <div className="voucher-admin-switches"><label><input type="checkbox" checked={form.publicVisible} onChange={(e) => update('publicVisible', e.target.checked)} /> Hiển thị trên carousel trang chủ</label><label><input type="checkbox" checked={form.active} onChange={(e) => update('active', e.target.checked)} /> Đang hoạt động</label></div>
                <div className="row modal-actions"><button className="btn" type="submit">{editingId ? 'Lưu thay đổi' : 'Tạo voucher'}</button><button className="btn btn-secondary" type="button" onClick={() => setOpen(false)}>Hủy</button></div>
            </form>
        </Modal>
    </div>;
}
