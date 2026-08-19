import { FormEvent, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ApiError, api } from '../lib/api';

export function ResetPasswordPage() {
    const [params] = useSearchParams();
    const token = params.get('token') ?? '';
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState('');
    const [error, setError] = useState(token ? '' : 'Liên kết đặt lại mật khẩu không hợp lệ.');
    const [saving, setSaving] = useState(false);

    const submit = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        setMessage('');
        if (newPassword !== confirmPassword) {
            setError('Mật khẩu xác nhận không khớp.');
            return;
        }
        setSaving(true);
        try {
            const result = await api<{ message: string }>('/auth/reset-password', {
                method: 'POST',
                body: JSON.stringify({ token, newPassword, confirmPassword }),
            });
            setMessage(result.message);
            setNewPassword('');
            setConfirmPassword('');
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể đặt lại mật khẩu.');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="auth-page auth-with-layout">
            <div className="auth-showcase">
                <span className="eyebrow">MẬT KHẨU MỚI</span>
                <h2>Đặt lại mật khẩu.<br />Tiếp tục CineVerse.</h2>
                <p>Chọn mật khẩu mới tối thiểu 8 ký tự và giữ thông tin đăng nhập an toàn.</p>
            </div>
            <form className="auth-card" onSubmit={submit}>
                <h1>Tạo mật khẩu mới</h1>
                {error && <div className="alert alert-error">{error}</div>}
                {message && <div className="alert alert-success">{message}</div>}
                <label>
                    Mật khẩu mới
                    <input
                        type="password"
                        minLength={8}
                        value={newPassword}
                        onChange={(event) => setNewPassword(event.target.value)}
                        autoComplete="new-password"
                        required
                    />
                </label>
                <label>
                    Xác nhận mật khẩu mới
                    <input
                        type="password"
                        minLength={8}
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                        autoComplete="new-password"
                        required
                    />
                </label>
                <button className="btn btn-block" disabled={!token || saving}>
                    {saving ? 'Đang cập nhật...' : 'Đặt lại mật khẩu'}
                </button>
                <p><Link to="/login">Quay lại đăng nhập</Link></p>
            </form>
        </div>
    );
}
