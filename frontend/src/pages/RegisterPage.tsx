import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../lib/api';
export function RegisterPage() {
  const { register } = useAuth();
  const nav = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await register(fullName, email, password);
      nav('/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không thể đăng ký.');
    }
  };
  return (
    <div className="auth-page auth-with-layout">
      <div className="auth-showcase">
        <span className="eyebrow">THÀNH VIÊN MỚI</span>
        <h2>
          Một tài khoản.
          <br />
          Mọi booking.
        </h2>
        <p>Tạo tài khoản để giữ ghế, thanh toán mô phỏng và quản lý vé tập trung.</p>
      </div>
      <form className="auth-card" onSubmit={submit}>
        <h1>Tạo tài khoản</h1>
        {error && <div className="alert alert-error">{error}</div>}
        <label>
          Họ và tên
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Mật khẩu
          <input
            type="password"
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button className="btn btn-block">Tạo tài khoản</button>
        <p>
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </p>
      </form>
    </div>
  );
}
