import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, api } from '../lib/api';

export function ForgotPasswordPage() {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [sending, setSending] = useState(false);

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        setMessage('');
        setSending(true);
        try {
            const result = await api<{ message: string }>('/auth/forgot-password', {
                method: 'POST',
                body: JSON.stringify({ email }),
            });
            setMessage(result.message);
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể gửi email lúc này.');
        } finally {
            setSending(false);
        }
    };

    return (
        <div className="auth-page auth-with-layout">
            <div className="auth-showcase">
                <span className="eyebrow">KHÔI PHỤC TÀI KHOẢN</span>
                <h2>Lấy lại quyền truy cập.<br />Không mất lịch sử vé.</h2>
                <p>CineVerse sẽ gửi liên kết đặt lại mật khẩu đến email đã đăng ký.</p>
            </div>
            <form className="auth-card" onSubmit={submit}>
                <h1>Quên mật khẩu</h1>
                <p>Nhập email tài khoản. Liên kết đặt lại mật khẩu có hiệu lực 30 phút.</p>
                {error && <div className="alert alert-error">{error}</div>}
                {message && <div className="alert alert-success">{message}</div>}
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
                <button className="btn btn-block" disabled={sending}>
                    {sending ? 'Đang gửi...' : 'Gửi liên kết đặt lại mật khẩu'}
                </button>
                <p><Link to="/login">← Quay lại đăng nhập</Link></p>
            </form>
        </div>
    );
}
