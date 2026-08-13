import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError } from '../lib/api';
import { useAuth } from '../hooks/useAuth';
export function LoginPage() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState('customer@cineverse.vn');
  const [password, setPassword] = useState('Customer@123');
  const [error, setError] = useState('');
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const u = await login(email, password);
      nav(u.role === 'ADMIN' ? '/admin' : '/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không thể đăng nhập.');
    }
  };
  return (
    <div className="auth-page auth-with-layout">
      <div className="auth-showcase">
        <span className="eyebrow">CINEVERSE MEMBER</span>
        <h2>
          Đặt ghế nhanh.
          <br />
          Giữ vé an toàn.
        </h2>
        <p>Đăng nhập để tiếp tục booking, quản lý vé và lịch sử thanh toán.</p>
      </div>
      <form className="auth-card" onSubmit={submit}>
        <h1>Chào mừng trở lại</h1>
        <p>Đăng nhập vào CineVerse.</p>
        {error && <div className="alert alert-error">{error}</div>}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Mật khẩu
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button className="btn btn-block">Đăng nhập</button>
        <p>
          Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link>
        </p>
      </form>
    </div>
  );
}
