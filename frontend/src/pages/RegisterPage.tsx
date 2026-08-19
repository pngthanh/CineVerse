import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../lib/api';
import { loadVietnamAddresses, type VietnamProvince } from '../lib/vietnamAddress';

export function RegisterPage() {
    const { register } = useAuth();
    const navigate = useNavigate();
    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [phone, setPhone] = useState('');
    const [provinces, setProvinces] = useState<VietnamProvince[]>([]);
    const [provinceCode, setProvinceCode] = useState('');
    const [districtCode, setDistrictCode] = useState('');
    const [wardCode, setWardCode] = useState('');
    const [addressDetail, setAddressDetail] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        let active = true;
        void loadVietnamAddresses().then(data => { if (active) setProvinces(data); })
            .catch(() => { if (active) setError('Không thể tải danh sách tỉnh/thành.'); });
        return () => { active = false; };
    }, []);

    const province = useMemo(() => provinces.find(item => String(item.code) === provinceCode), [provinces, provinceCode]);
    const district = useMemo(() => province?.districts.find(item => String(item.code) === districtCode), [province, districtCode]);
    const ward = useMemo(() => district?.wards.find(item => String(item.code) === wardCode), [district, wardCode]);

    const submit = async (event: FormEvent) => {
        event.preventDefault(); setError('');
        if (password !== confirmPassword) { setError('Mật khẩu xác nhận không khớp.'); return; }
        if (!province || !district || !ward) { setError('Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện và Phường/Xã.'); return; }
        try {
            await register({
                fullName, email, username, password, confirmPassword, phone,
                provinceCode, provinceName: province.name,
                districtCode, districtName: district.name,
                wardCode, wardName: ward.name, addressDetail,
            });
            navigate('/');
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể đăng ký.');
        }
    };

    return <div className="auth-page auth-with-layout">
        <div className="auth-showcase"><span className="eyebrow">THÀNH VIÊN MỚI</span><h2>Một tài khoản.<br />Mọi booking.</h2><p>Tạo tài khoản để đặt vé và quản lý thông tin cá nhân tập trung.</p></div>
        <form className="auth-card auth-card-wide" onSubmit={submit}>
            <h1>Tạo tài khoản</h1>{error && <div className="alert alert-error">{error}</div>}
            <div className="form-two-col">
                <label>Họ và tên<input value={fullName} onChange={e => setFullName(e.target.value)} required /></label>
                <label>Số điện thoại<input value={phone} onChange={e => setPhone(e.target.value)} placeholder="09xxxxxxxx" required /></label>
            </div>
            <div className="form-two-col">
                <label>Email<input type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="email" required /></label>
                <label>Username<input value={username} onChange={e => setUsername(e.target.value)} minLength={4} maxLength={40} pattern="[A-Za-z0-9._-]+" autoComplete="username" required /></label>
            </div>
            <div className="form-two-col">
                <label>Mật khẩu<input type="password" minLength={8} value={password} onChange={e => setPassword(e.target.value)} autoComplete="new-password" required /></label>
                <label>Nhập lại mật khẩu<input type="password" minLength={8} value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} autoComplete="new-password" required /></label>
            </div>
            <div className="form-three-col address-grid">
                <label>Tỉnh / Thành<select value={provinceCode} onChange={e => { setProvinceCode(e.target.value); setDistrictCode(''); setWardCode(''); }} required><option value="">Chọn tỉnh/thành</option>{provinces.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
                <label>Quận / Huyện<select value={districtCode} disabled={!province} onChange={e => { setDistrictCode(e.target.value); setWardCode(''); }} required><option value="">Chọn quận/huyện</option>{province?.districts.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
                <label>Phường / Xã<select value={wardCode} disabled={!district} onChange={e => setWardCode(e.target.value)} required><option value="">Chọn phường/xã</option>{district?.wards.map(item => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
            </div>
            <label>Địa chỉ chi tiết<input value={addressDetail} onChange={e => setAddressDetail(e.target.value)} placeholder="Số nhà, tên đường... (không bắt buộc)" /></label>
            <button className="btn btn-block">Tạo tài khoản</button><p>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
        </form>
    </div>;
}
