import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Modal } from '../../components/Modal';
import { ApiError, api } from '../../lib/api';
import { money } from '../../lib/format';
import type { AdminConcessionItem } from '../../types';

type ConcessionForm = {
    name: string;
    description: string;
    price: number;
    active: boolean;
};

const emptyForm = (): ConcessionForm => ({ name: '', description: '', price: 0, active: true });

export function AdminConcessionsPage() {
    const [items, setItems] = useState<AdminConcessionItem[]>([]);
    const [form, setForm] = useState<ConcessionForm>(emptyForm);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState('');
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const load = async () => setItems(await api<AdminConcessionItem[]>('/admin/concessions'));

    useEffect(() => {
        let active = true;
        void api<AdminConcessionItem[]>('/admin/concessions')
            .then((data) => { if (active) setItems(data); })
            .catch(() => { if (active) setError('Không thể tải danh sách bắp nước.'); });
        return () => { active = false; };
    }, []);

    const filtered = useMemo(() => {
        const keyword = query.trim().toLowerCase();
        if (!keyword) return items;
        return items.filter((item) => `${item.name} ${item.description ?? ''}`.toLowerCase().includes(keyword));
    }, [items, query]);

    const startCreate = () => {
        setEditingId(null); setForm(emptyForm()); setError(''); setMessage(''); setOpen(true);
    };

    const startEdit = (item: AdminConcessionItem) => {
        setEditingId(item.id);
        setForm({ name: item.name, description: item.description ?? '', price: item.price, active: item.active });
        setError(''); setMessage(''); setOpen(true);
    };

    const submit = async (event: FormEvent) => {
        event.preventDefault(); setError(''); setMessage('');
        try {
            await api(editingId ? `/admin/concessions/${editingId}` : '/admin/concessions', {
                method: editingId ? 'PUT' : 'POST',
                body: JSON.stringify(form),
            });
            setOpen(false);
            setMessage(editingId ? 'Đã cập nhật món.' : 'Đã thêm món mới.');
            await load();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể lưu món bắp nước.');
        }
    };

    const deactivate = async (item: AdminConcessionItem) => {
        if (!window.confirm(`Ngừng bán ${item.name}? Món này sẽ không còn xuất hiện ở bước checkout.`)) return;
        try {
            await api(`/admin/concessions/${item.id}`, { method: 'DELETE' });
            setMessage(`Đã ngừng bán ${item.name}.`); await load();
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể ngừng bán món.');
        }
    };

    return <div className="admin-page">
        <div className="page-title admin-page-title-row">
            <div><h1>Bắp nước & combo</h1><p>Quản lý món bán kèm. Giá được backend chốt lại khi khách tạo booking.</p></div>
            <button className="btn" type="button" onClick={startCreate}>+ Thêm món</button>
        </div>
        {error && <div className="alert alert-error">{error}</div>}
        {message && <div className="alert alert-success">{message}</div>}
        <section className="panel concession-admin-toolbar">
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm bắp, nước hoặc combo..." />
            <span>{filtered.length} món</span>
        </section>
        <section className="concession-admin-grid">
            {filtered.map((item) => <article className={`concession-admin-card ${!item.active ? 'inactive' : ''}`} key={item.id}>
                <div className="concession-admin-card-head"><span className="concession-admin-icon">{item.name.toLowerCase().includes('combo') ? 'CB' : 'CV'}</span><span className={`status-pill ${item.active ? 'success' : 'muted'}`}>{item.active ? 'Đang bán' : 'Đã tắt'}</span></div>
                <div><h3>{item.name}</h3><p>{item.description || 'Chưa có mô tả.'}</p></div>
                <strong className="concession-admin-price">{money(item.price)}</strong>
                <div className="row concession-admin-actions"><button className="btn btn-sm btn-secondary" type="button" onClick={() => startEdit(item)}>Sửa</button><button className="btn btn-sm btn-danger" type="button" disabled={!item.active} onClick={() => void deactivate(item)}>Ngừng bán</button></div>
            </article>)}
            {!filtered.length && <div className="panel empty">Không có món phù hợp.</div>}
        </section>

        <Modal open={open} title={editingId ? 'Chỉnh sửa món' : 'Thêm bắp nước / combo'} onClose={() => setOpen(false)}>
            <form className="modal-form admin-clean-form" onSubmit={submit}>
                <label>Tên món<input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} maxLength={80} placeholder="Ví dụ: Combo CineVerse" required /></label>
                <label>Mô tả<textarea value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} maxLength={220} placeholder="Ví dụ: 1 bắp rang bơ + 2 nước ngọt" /></label>
                <label>Giá bán (VNĐ)<input type="number" min="1000" step="1000" value={form.price || ''} onChange={(event) => setForm((current) => ({ ...current, price: Number(event.target.value) }))} placeholder="99000" required /></label>
                <label className="admin-check-row"><input type="checkbox" checked={form.active} onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))} /> Đang bán</label>
                <div className="row modal-actions"><button className="btn" type="submit">{editingId ? 'Lưu thay đổi' : 'Thêm món'}</button><button className="btn btn-secondary" type="button" onClick={() => setOpen(false)}>Hủy</button></div>
            </form>
        </Modal>
    </div>;
}
