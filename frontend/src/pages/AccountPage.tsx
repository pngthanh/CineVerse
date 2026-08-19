import { FormEvent, useEffect, useMemo, useState } from 'react';
import { api, ApiError } from '../lib/api';
import { roleLabel, statusLabel } from '../lib/format';
import { loadVietnamAddresses, type VietnamProvince } from '../lib/vietnamAddress';
import type { UserProfile } from '../types';

export function AccountPage() {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [activeTab, setActiveTab] = useState<'profile' | 'security'>('profile');
    const [fullName, setFullName] = useState(''); const [phone, setPhone] = useState('');
    const [provinces, setProvinces] = useState<VietnamProvince[]>([]);
    const [provinceCode, setProvinceCode] = useState(''); const [districtCode, setDistrictCode] = useState(''); const [wardCode, setWardCode] = useState('');
    const [addressDetail, setAddressDetail] = useState('');
    const [profileMessage, setProfileMessage] = useState(''); const [securityMessage, setSecurityMessage] = useState(''); const [error, setError] = useState('');
    const [currentPassword, setCurrentPassword] = useState(''); const [newPassword, setNewPassword] = useState(''); const [confirmPassword, setConfirmPassword] = useState('');

    useEffect(() => {
        let active = true;
        void Promise.all([api<UserProfile>('/me'), loadVietnamAddresses()]).then(([data, addressData]) => {
            if (!active) return;
            setProfile(data); setFullName(data.fullName); setPhone(data.phone ?? ''); setProvinces(addressData);
            setProvinceCode(data.provinceCode ?? ''); setDistrictCode(data.districtCode ?? ''); setWardCode(data.wardCode ?? ''); setAddressDetail(data.addressDetail ?? '');
        }).catch(() => { if (active) setError('Không thể tải thông tin tài khoản hoặc dữ liệu địa chỉ.'); });
        return () => { active = false; };
    }, []);

    const province = useMemo(() => provinces.find(item => String(item.code) === provinceCode), [provinces, provinceCode]);
    const district = useMemo(() => province?.districts.find(item => String(item.code) === districtCode), [province, districtCode]);
    const ward = useMemo(() => district?.wards.find(item => String(item.code) === wardCode), [district, wardCode]);

    const saveProfile = async (event: FormEvent) => {
        event.preventDefault(); setError(''); setProfileMessage('');
        if (!province || !district || !ward) { setError('Vui lòng chọn đầy đủ địa chỉ.'); return; }
        try {
            const updated = await api<UserProfile>('/me', { method: 'PATCH', body: JSON.stringify({
                fullName, phone, provinceCode, provinceName: province.name, districtCode, districtName: district.name, wardCode, wardName: ward.name, addressDetail,
            }) });
            setProfile(updated); setProfileMessage('Đã cập nhật hồ sơ.');
        } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể cập nhật hồ sơ.'); }
    };

    const changePassword = async (event: FormEvent) => {
        event.preventDefault(); setError(''); setSecurityMessage('');
        if (newPassword !== confirmPassword) { setError('Mật khẩu mới và xác nhận mật khẩu không khớp.'); return; }
        try {
            const result = await api<{ message: string }>('/me/password', { method: 'POST', body: JSON.stringify({ currentPassword, newPassword, confirmPassword }) });
            setSecurityMessage(result.message); setCurrentPassword(''); setNewPassword(''); setConfirmPassword('');
        } catch (requestError) { setError(requestError instanceof ApiError ? requestError.message : 'Không thể đổi mật khẩu.'); }
    };

    return <div className="container page account-page">
        <div className="page-title"><span className="eyebrow">CINEVERSE ACCOUNT</span><h1>Tài khoản của tôi</h1><p>Quản lý hồ sơ, địa chỉ và bảo mật tài khoản CineVerse.</p></div>
        <div className="account-shell">
            <aside className="account-sidebar panel"><div className="account-avatar">{profile?.fullName?.charAt(0).toUpperCase() ?? 'C'}</div><strong>{profile?.fullName ?? 'CineVerse Member'}</strong><span>{profile?.email ?? 'Đang tải...'}</span>
                <div className="account-tabs"><button className={activeTab === 'profile' ? 'active' : ''} onClick={() => setActiveTab('profile')}>Thông tin cá nhân</button><button className={activeTab === 'security' ? 'active' : ''} onClick={() => setActiveTab('security')}>Bảo mật</button><button disabled>Voucher đã lưu · sắp có</button><button disabled>Liên kết tài khoản · sắp có</button></div>
                <div className="account-meta"><span>Vai trò <strong>{roleLabel(profile?.role)}</strong></span><span>Trạng thái <strong>{statusLabel(profile?.status)}</strong></span></div>
            </aside>
            <section className="account-content panel">{error && <div className="alert alert-error">{error}</div>}
                {activeTab === 'profile' && <form onSubmit={saveProfile}><div className="account-section-head"><div><span className="section-kicker">HỒ SƠ</span><h2>Thông tin cá nhân</h2><p>Cập nhật thông tin liên hệ và địa chỉ của bạn.</p></div></div>
                    <div className="form-two-col"><label>Họ và tên<input value={fullName} onChange={e => setFullName(e.target.value)} required /></label><label>Số điện thoại<input value={phone} onChange={e => setPhone(e.target.value)} required /></label></div>
                    <div className="form-two-col"><label>Username<input value={profile?.username ?? ''} readOnly /><small>Username đã tạo không thể thay đổi.</small></label><label>Email<input value={profile?.email ?? ''} readOnly /></label></div>
                    <div className="form-three-col address-grid"><label>Tỉnh / Thành<select value={provinceCode} onChange={e => { setProvinceCode(e.target.value); setDistrictCode(''); setWardCode(''); }} required><option value="">Chọn tỉnh/thành</option>{provinces.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label><label>Quận / Huyện<select value={districtCode} disabled={!province} onChange={e => { setDistrictCode(e.target.value); setWardCode(''); }} required><option value="">Chọn quận/huyện</option>{province?.districts.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label><label>Phường / Xã<select value={wardCode} disabled={!district} onChange={e => setWardCode(e.target.value)} required><option value="">Chọn phường/xã</option>{district?.wards.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label></div>
                    <label>Địa chỉ chi tiết<input value={addressDetail} onChange={e => setAddressDetail(e.target.value)} placeholder="Số nhà, tên đường..." /></label>
                    {profileMessage && <div className="alert alert-success">{profileMessage}</div>}<button className="btn">Lưu thay đổi</button></form>}
                {activeTab === 'security' && <form onSubmit={changePassword}><div className="account-section-head"><div><span className="section-kicker">BẢO MẬT</span><h2>Đổi mật khẩu</h2><p>Mật khẩu mới phải có ít nhất 8 ký tự.</p></div></div><label>Mật khẩu hiện tại<input type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required /></label><div className="form-two-col"><label>Mật khẩu mới<input type="password" minLength={8} value={newPassword} onChange={e => setNewPassword(e.target.value)} required /></label><label>Xác nhận mật khẩu mới<input type="password" minLength={8} value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} required /></label></div>{securityMessage && <div className="alert alert-success">{securityMessage}</div>}<button className="btn">Cập nhật mật khẩu</button></form>}
            </section>
        </div>
    </div>;
}
