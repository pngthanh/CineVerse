import { FormEvent, useEffect, useMemo, useState } from 'react';
import { GoogleIdentityButton } from '../components/GoogleIdentityButton';
import { api, ApiError } from '../lib/api';
import { money, roleLabel, statusLabel } from '../lib/format';
import { loadVietnamAddresses, type VietnamProvince } from '../lib/vietnamAddress';
import type { UserProfile, Voucher } from '../types';

export function AccountPage() {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'linked' | 'vouchers'>('profile');
    const [fullName, setFullName] = useState('');
    const [phone, setPhone] = useState('');
    const [email, setEmail] = useState('');
    const [provinces, setProvinces] = useState<VietnamProvince[]>([]);
    const [provinceCode, setProvinceCode] = useState('');
    const [districtCode, setDistrictCode] = useState('');
    const [wardCode, setWardCode] = useState('');
    const [addressDetail, setAddressDetail] = useState('');
    const [savedVouchers, setSavedVouchers] = useState<Voucher[]>([]);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [newUsername, setNewUsername] = useState('');
    const [localPassword, setLocalPassword] = useState('');
    const [localConfirmPassword, setLocalConfirmPassword] = useState('');

    useEffect(() => {
        let active = true;
        void Promise.all([api<UserProfile>('/me'), loadVietnamAddresses(), api<Voucher[]>('/vouchers/saved')])
            .then(([data, addressData, voucherData]) => {
                if (!active) return;
                setProfile(data); setFullName(data.fullName); setPhone(data.phone ?? ''); setEmail(data.email ?? '');
                setProvinces(addressData); setProvinceCode(data.provinceCode ?? ''); setDistrictCode(data.districtCode ?? '');
                setWardCode(data.wardCode ?? ''); setAddressDetail(data.addressDetail ?? ''); setSavedVouchers(voucherData);
            })
            .catch(() => { if (active) setError('Không thể tải đầy đủ dữ liệu tài khoản.'); });
        return () => { active = false; };
    }, []);

    const province = useMemo(() => provinces.find((item) => String(item.code) === provinceCode), [provinces, provinceCode]);
    const district = useMemo(() => province?.districts.find((item) => String(item.code) === districtCode), [province, districtCode]);
    const ward = useMemo(() => district?.wards.find((item) => String(item.code) === wardCode), [district, wardCode]);

    const saveProfile = async (event: FormEvent) => {
        event.preventDefault(); setError(''); setMessage('');
        const hasAddressSelection = Boolean(provinceCode || districtCode || wardCode);
        if (hasAddressSelection && (!province || !district || !ward)) { setError('Nếu cập nhật địa chỉ, vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện và Phường/Xã.'); return; }
        try {
            const updated = await api<UserProfile>('/me', { method: 'PATCH', body: JSON.stringify({ fullName, phone, email, provinceCode: province?.code ? String(province.code) : '', provinceName: province?.name ?? '', districtCode: district?.code ? String(district.code) : '', districtName: district?.name ?? '', wardCode: ward?.code ? String(ward.code) : '', wardName: ward?.name ?? '', addressDetail }) });
            setProfile(updated); setEmail(updated.email ?? ''); setMessage('Đã cập nhật hồ sơ.');
        } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể cập nhật hồ sơ.'); }
    };
    const changePassword = async (event: FormEvent) => { event.preventDefault(); setError(''); setMessage(''); if (newPassword !== confirmPassword) { setError('Mật khẩu mới và xác nhận mật khẩu không khớp.'); return; } try { const result = await api<{ message: string }>('/me/password', { method: 'POST', body: JSON.stringify({ currentPassword, newPassword, confirmPassword }) }); setMessage(result.message); setCurrentPassword(''); setNewPassword(''); setConfirmPassword(''); } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể đổi mật khẩu.'); } };
    const createLocalCredentials = async (event: FormEvent) => { event.preventDefault(); setError(''); setMessage(''); if (localPassword !== localConfirmPassword) { setError('Mật khẩu và xác nhận mật khẩu không khớp.'); return; } try { const updated = await api<UserProfile>('/me/local-credentials', { method: 'POST', body: JSON.stringify({ username: newUsername, password: localPassword, confirmPassword: localConfirmPassword }) }); setProfile(updated); setMessage('Đã tạo username và mật khẩu CineVerse.'); setNewUsername(''); setLocalPassword(''); setLocalConfirmPassword(''); } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể tạo tài khoản đăng nhập CineVerse.'); } };
    const linkGoogle = async (credential: string) => { setError(''); setMessage(''); try { const updated = await api<UserProfile>('/me/google/link', { method: 'POST', body: JSON.stringify({ credential }) }); setProfile(updated); setEmail(updated.email ?? ''); setMessage('Đã liên kết tài khoản Google.'); } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể liên kết Google.'); } };
    const unlinkGoogle = async () => { if (!window.confirm('Ngắt liên kết tài khoản Google khỏi CineVerse?')) return; setError(''); setMessage(''); try { const updated = await api<UserProfile>('/me/google/link', { method: 'DELETE' }); setProfile(updated); setEmail(updated.email ?? ''); setMessage('Đã ngắt liên kết Google.'); } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể ngắt liên kết Google.'); } };
    const unsaveVoucher = async (voucher: Voucher) => { try { await api(`/vouchers/${voucher.id}/save`, { method: 'DELETE' }); setSavedVouchers((items) => items.filter((item) => item.id !== voucher.id)); setMessage(`Đã bỏ lưu mã ${voucher.code}.`); } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể bỏ lưu voucher.'); } };
    const switchTab = (tab: typeof activeTab) => { setActiveTab(tab); setError(''); setMessage(''); };

    return <div className="container page account-page">
        <div className="page-title"><span className="eyebrow">CINEVERSE ACCOUNT</span><h1>Tài khoản của tôi</h1><p>Quản lý hồ sơ, voucher, phương thức đăng nhập và bảo mật CineVerse.</p></div>
        <div className="account-shell">
            <aside className="account-sidebar panel"><div className="account-avatar">{profile?.fullName?.charAt(0).toUpperCase() ?? 'C'}</div><strong>{profile?.fullName ?? 'CineVerse Member'}</strong><span>{profile?.email || profile?.googleEmail || 'Chưa thêm email'}</span><div className="account-tabs"><button className={activeTab === 'profile' ? 'active' : ''} onClick={() => switchTab('profile')}>Thông tin cá nhân</button><button className={activeTab === 'security' ? 'active' : ''} onClick={() => switchTab('security')}>Bảo mật</button><button className={activeTab === 'linked' ? 'active' : ''} onClick={() => switchTab('linked')}>Liên kết tài khoản</button><button className={activeTab === 'vouchers' ? 'active' : ''} onClick={() => switchTab('vouchers')}>Voucher đã lưu <span className="tab-count">{savedVouchers.length}</span></button></div><div className="account-meta"><span>Vai trò <strong>{roleLabel(profile?.role)}</strong></span><span>Trạng thái <strong>{statusLabel(profile?.status)}</strong></span></div></aside>
            <section className="account-content panel">
                {error && <div className="alert alert-error">{error}</div>}{message && <div className="alert alert-success">{message}</div>}
                {activeTab === 'profile' && <form onSubmit={saveProfile}><div className="account-section-head"><div><span className="section-kicker">HỒ SƠ</span><h2>Thông tin cá nhân</h2><p>Email khôi phục và địa chỉ đều có thể bổ sung sau.</p></div></div><div className="form-two-col"><label>Họ và tên<input value={fullName} onChange={(e) => setFullName(e.target.value)} required /></label><label>Số điện thoại<input value={phone} onChange={(e) => setPhone(e.target.value)} required /></label></div><div className="form-two-col"><label>Username<input value={profile?.username ?? ''} readOnly placeholder={profile?.localCredentials ? '' : 'Chưa tạo'} /><small>{profile?.localCredentials ? 'Username đã tạo không thể thay đổi.' : 'Tạo username trong tab Bảo mật.'}</small></label><label>Email khôi phục<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder={profile?.googleEmail ?? 'you@example.com'} /><small>Không bắt buộc. Dùng để đăng nhập bằng email và nhận liên kết quên mật khẩu.</small></label></div><div className="form-three-col address-grid"><label>Tỉnh / Thành<select value={provinceCode} onChange={(e) => { setProvinceCode(e.target.value); setDistrictCode(''); setWardCode(''); }}><option value="">Chưa cập nhật</option>{provinces.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label><label>Quận / Huyện<select value={districtCode} disabled={!province} onChange={(e) => { setDistrictCode(e.target.value); setWardCode(''); }}><option value="">Chưa cập nhật</option>{province?.districts.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label><label>Phường / Xã<select value={wardCode} disabled={!district} onChange={(e) => setWardCode(e.target.value)}><option value="">Chưa cập nhật</option>{district?.wards.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label></div><label>Địa chỉ chi tiết<input value={addressDetail} onChange={(e) => setAddressDetail(e.target.value)} placeholder="Số nhà, tên đường..." /></label><button className="btn">Lưu thay đổi</button></form>}
                {activeTab === 'security' && (profile?.localCredentials ? <form onSubmit={changePassword}><div className="account-section-head"><div><span className="section-kicker">BẢO MẬT</span><h2>Đổi mật khẩu</h2><p>Username của bạn là <strong>{profile.username}</strong>.</p></div></div><label>Mật khẩu hiện tại<input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required /></label><div className="form-two-col"><label>Mật khẩu mới<input type="password" minLength={8} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required /></label><label>Xác nhận mật khẩu mới<input type="password" minLength={8} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required /></label></div><button className="btn">Cập nhật mật khẩu</button></form> : <form onSubmit={createLocalCredentials}><div className="account-section-head"><div><span className="section-kicker">BẢO MẬT</span><h2>Tạo đăng nhập CineVerse</h2><p>Tài khoản hiện dùng Google. Bạn có thể tạo thêm username và mật khẩu.</p></div></div><label>Username<input value={newUsername} onChange={(e) => setNewUsername(e.target.value)} minLength={4} maxLength={40} pattern="[A-Za-z0-9._-]+" required /></label><div className="form-two-col"><label>Mật khẩu mới<input type="password" minLength={8} value={localPassword} onChange={(e) => setLocalPassword(e.target.value)} required /></label><label>Xác nhận mật khẩu<input type="password" minLength={8} value={localConfirmPassword} onChange={(e) => setLocalConfirmPassword(e.target.value)} required /></label></div><button className="btn">Tạo username & mật khẩu</button></form>)}
                {activeTab === 'linked' && <div><div className="account-section-head"><div><span className="section-kicker">TÀI KHOẢN LIÊN KẾT</span><h2>Google</h2><p>Liên kết Google để đăng nhập nhanh mà vẫn giữ nguyên dữ liệu CineVerse.</p></div></div>{profile?.googleLinked ? <div className="linked-account-card"><div className="google-mark">G</div><div><strong>Google đã liên kết</strong><span>{profile.googleEmail}</span></div><button className="btn btn-secondary btn-sm" type="button" onClick={() => void unlinkGoogle()}>Ngắt liên kết</button></div> : <div className="linked-account-empty"><p>Chưa có tài khoản Google liên kết.</p><GoogleIdentityButton onCredential={linkGoogle} /></div>}{!profile?.localCredentials && profile?.googleLinked && <p className="muted linked-account-warning">Google đang là phương thức đăng nhập duy nhất. Hãy tạo username và mật khẩu trước nếu muốn ngắt liên kết.</p>}</div>}
                {activeTab === 'vouchers' && <div><div className="account-section-head"><div><span className="section-kicker">ƯU ĐÃI ĐÃ LƯU</span><h2>Voucher của tôi</h2><p>Các mã đã lưu sẽ xuất hiện nhanh ở bước thanh toán.</p></div></div>{savedVouchers.length ? <div className="saved-voucher-grid">{savedVouchers.map((voucher) => <article className="saved-voucher-card" key={voucher.id}><div><span className="voucher-chip">{voucher.discountType === 'PERCENT' ? `-${voucher.discountValue}%` : `-${money(voucher.discountValue)}`}</span><h3>{voucher.title}</h3><code>{voucher.code}</code><p>{voucher.movieTitle ? `Áp dụng cho ${voucher.movieTitle}` : 'Áp dụng cho các phim đủ điều kiện'}</p><small>Hết hạn {new Date(voucher.expiresAt).toLocaleString('vi-VN')}</small></div><button className="btn btn-secondary btn-sm" type="button" onClick={() => void unsaveVoucher(voucher)}>Bỏ lưu</button></article>)}</div> : <div className="empty-state compact"><h3>Chưa lưu voucher nào</h3><p>Vào trang chủ và bấm “Lưu mã” để voucher xuất hiện tại đây.</p></div>}</div>}
            </section>
        </div>
    </div>;
}
