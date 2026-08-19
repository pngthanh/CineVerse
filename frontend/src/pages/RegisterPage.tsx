import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../lib/api';

export function RegisterPage() {
    const { register } = useAuth();
    const navigate = useNavigate();
    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        if (password !== confirmPassword) {
            setError('Mật khẩu xác nhận không khớp.');
            return;
        }
        try {
            await register({ fullName, email, username, password, confirmPassword });
            navigate('/');
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể đăng ký.');
        }
    };

    return (
        <div className="auth-page auth-with-layout">
            <div className="auth-showcase">
                <span className="eyebrow">THÀNH VIÊN MỚI</span>
                <h2>Một tài khoản.<br />Mọi booking.</h2>
                <p>Tạo tài khoản để giữ ghế, thanh toán và quản lý vé tập trung.</p>
            </div>
            <form className="auth-card" onSubmit={submit}>
                <h1>Tạo tài khoản</h1>
                {error && <div className="alert alert-error">{error}</div>}
                <label>
                    Họ và tên
                    <input value={fullName} onChange={(event) => setFullName(event.target.value)} required />
                </label>
                <label>
                    Email
                    <input
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        autoComplete="email"
                        required
                    />
                </label>
                <label>
                    Username
                    <input
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                        minLength={4}
                        maxLength={40}
                        pattern="[A-Za-z0-9._-]+"
                        autoComplete="username"
                        required
                    />
                    <small>4–40 ký tự, dùng chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.</small>
                </label>
                <label>
                    Mật khẩu
                    <input
                        type="password"
                        minLength={8}
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        autoComplete="new-password"
                        required
                    />
                </label>
                <label>
                    Nhập lại mật khẩu
                    <input
                        type="password"
                        minLength={8}
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                        autoComplete="new-password"
                        required
                    />
                </label>
                <button className="btn btn-block">Tạo tài khoản</button>
                <p>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
            </form>
        </div>
    );
}
