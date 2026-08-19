import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../lib/api';

export function RegisterPage() {
    const { register } = useAuth();
    const navigate = useNavigate();
    const [fullName, setFullName] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [phone, setPhone] = useState('');
    const [error, setError] = useState('');

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        if (password !== confirmPassword) {
            setError('Mật khẩu xác nhận không khớp.');
            return;
        }
        try {
            await register({ fullName, username, password, confirmPassword, phone });
            navigate('/');
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể đăng ký.');
        }
    };

    return <div className="auth-page auth-with-layout">
        <div className="auth-showcase">
            <span className="eyebrow">THÀNH VIÊN MỚI</span>
            <h2>Một tài khoản.<br />Mọi booking.</h2>
            <p>Tạo tài khoản nhanh. Email khôi phục và địa chỉ có thể bổ sung sau trong Tài khoản của tôi.</p>
        </div>
        <form className="auth-card" onSubmit={submit}>
            <h1>Tạo tài khoản</h1>
            {error && <div className="alert alert-error">{error}</div>}
            <label>Họ và tên<input value={fullName} onChange={(e) => setFullName(e.target.value)} required /></label>
            <label>Số điện thoại<input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="09xxxxxxxx" required /></label>
            <label>Username<input value={username} onChange={(e) => setUsername(e.target.value)} minLength={4} maxLength={40} pattern="[A-Za-z0-9._-]+" autoComplete="username" required /></label>
            <div className="form-two-col">
                <label>Mật khẩu<input type="password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" required /></label>
                <label>Nhập lại mật khẩu<input type="password" minLength={8} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} autoComplete="new-password" required /></label>
            </div>
            <button className="btn btn-block">Tạo tài khoản</button>
            <p>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
        </form>
    </div>;
}
