import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GoogleIdentityButton } from '../components/GoogleIdentityButton';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../lib/api';

export function LoginPage() {
    const { login, loginWithGoogle } = useAuth();
    const navigate = useNavigate();
    const [identifier, setIdentifier] = useState('customer');
    const [password, setPassword] = useState('Customer@123');
    const [error, setError] = useState('');

    const finishLogin = (role: string) => navigate(role === 'ADMIN' ? '/admin' : role === 'STAFF' ? '/staff' : '/');

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        try {
            const user = await login(identifier, password);
            finishLogin(user.role);
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể đăng nhập.');
        }
    };

    const googleLogin = async (credential: string) => {
        setError('');
        try {
            const user = await loginWithGoogle(credential);
            finishLogin(user.role);
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể đăng nhập bằng Google.');
        }
    };

    return (
        <div className="auth-page auth-with-layout">
            <div className="auth-showcase">
                <span className="eyebrow">CINEVERSE MEMBER</span>
                <h2>Đặt ghế nhanh.<br />Giữ vé an toàn.</h2>
                <p>Đăng nhập để tiếp tục booking, quản lý vé và lịch sử thanh toán.</p>
            </div>
            <form className="auth-card" onSubmit={submit}>
                <h1>Chào mừng trở lại</h1>
                <p>Đăng nhập bằng username, email hoặc Google.</p>
                {error && <div className="alert alert-error">{error}</div>}
                <GoogleIdentityButton onCredential={googleLogin} text="signin_with" />
                <div className="auth-divider"><span>hoặc</span></div>
                <label>
                    Username hoặc email
                    <input value={identifier} onChange={(event) => setIdentifier(event.target.value)} autoComplete="username" required />
                </label>
                <label>
                    Mật khẩu
                    <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required />
                </label>
                <div className="auth-inline-links"><Link to="/forgot-password">Quên mật khẩu?</Link></div>
                <button className="btn btn-block">Đăng nhập</button>
                <p>Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p>
            </form>
        </div>
    );
}
