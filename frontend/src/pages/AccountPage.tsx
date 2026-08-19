import { FormEvent, useEffect, useState } from 'react';
import { api, ApiError } from '../lib/api';
import { roleLabel, statusLabel } from '../lib/format';
import type { UserProfile } from '../types';

export function AccountPage() {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [name, setName] = useState('');
    const [activeTab, setActiveTab] = useState<'profile' | 'security'>('profile');
    const [profileMessage, setProfileMessage] = useState('');
    const [securityMessage, setSecurityMessage] = useState('');
    const [error, setError] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');

    useEffect(() => {
        let active = true;
        void api<UserProfile>('/me')
            .then((data) => {
                if (!active) return;
                setProfile(data);
                setName(data.fullName);
            })
            .catch(() => {
                if (active) setError('Không thể tải thông tin tài khoản.');
            });
        return () => {
            active = false;
        };
    }, []);

    const saveProfile = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        setProfileMessage('');
        try {
            const updated = await api<UserProfile>('/me', {
                method: 'PATCH',
                body: JSON.stringify({ fullName: name }),
            });
            setProfile(updated);
            setProfileMessage('Đã cập nhật hồ sơ.');
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể cập nhật hồ sơ.');
        }
    };

    const changePassword = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        setSecurityMessage('');
        if (newPassword !== confirmPassword) {
            setError('Mật khẩu mới và xác nhận mật khẩu không khớp.');
            return;
        }
        try {
            const result = await api<{ message: string }>('/me/password', {
                method: 'POST',
                body: JSON.stringify({ currentPassword, newPassword, confirmPassword }),
            });
            setSecurityMessage(result.message);
            setCurrentPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể đổi mật khẩu.');
        }
    };

    return (
        <div className="container page account-page">
            <div className="page-title">
                <span className="eyebrow">CINEVERSE ACCOUNT</span>
                <h1>Tài khoản của tôi</h1>
                <p>Quản lý hồ sơ và bảo mật tài khoản CineVerse.</p>
            </div>

            <div className="account-shell">
                <aside className="account-sidebar panel">
                    <div className="account-avatar">{profile?.fullName?.charAt(0).toUpperCase() ?? 'C'}</div>
                    <strong>{profile?.fullName ?? 'CineVerse Member'}</strong>
                    <span>{profile?.email ?? 'Đang tải...'}</span>
                    <div className="account-tabs">
                        <button
                            className={activeTab === 'profile' ? 'active' : ''}
                            onClick={() => setActiveTab('profile')}
                        >
                            Thông tin cá nhân
                        </button>
                        <button
                            className={activeTab === 'security' ? 'active' : ''}
                            onClick={() => setActiveTab('security')}
                        >
                            Bảo mật
                        </button>
                        <button disabled>Voucher đã lưu · sắp có</button>
                        <button disabled>Liên kết tài khoản · sắp có</button>
                    </div>
                    <div className="account-meta">
                        <span>Vai trò <strong>{roleLabel(profile?.role)}</strong></span>
                        <span>Trạng thái <strong>{statusLabel(profile?.status)}</strong></span>
                    </div>
                </aside>

                <section className="account-content panel">
                    {error && <div className="alert alert-error">{error}</div>}

                    {activeTab === 'profile' && (
                        <form onSubmit={saveProfile}>
                            <div className="account-section-head">
                                <div>
                                    <span className="section-kicker">HỒ SƠ</span>
                                    <h2>Thông tin cá nhân</h2>
                                </div>
                            </div>
                            <div className="form-two-col">
                                <label>
                                    Họ và tên
                                    <input value={name} onChange={(event) => setName(event.target.value)} required />
                                </label>
                                <label>
                                    Username
                                    <input value={profile?.username ?? ''} readOnly />
                                    <small>Username đã tạo không thể thay đổi.</small>
                                </label>
                            </div>
                            <label>
                                Email
                                <input value={profile?.email ?? ''} readOnly />
                            </label>
                            {profileMessage && <div className="alert alert-success">{profileMessage}</div>}
                            <button className="btn">Lưu thay đổi</button>
                        </form>
                    )}

                    {activeTab === 'security' && (
                        <form onSubmit={changePassword}>
                            <div className="account-section-head">
                                <div>
                                    <span className="section-kicker">BẢO MẬT</span>
                                    <h2>Đổi mật khẩu</h2>
                                    <p>Mật khẩu mới phải có ít nhất 8 ký tự.</p>
                                </div>
                            </div>
                            <label>
                                Mật khẩu hiện tại
                                <input
                                    type="password"
                                    value={currentPassword}
                                    onChange={(event) => setCurrentPassword(event.target.value)}
                                    autoComplete="current-password"
                                    required
                                />
                            </label>
                            <div className="form-two-col">
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
                            </div>
                            {securityMessage && <div className="alert alert-success">{securityMessage}</div>}
                            <button className="btn">Cập nhật mật khẩu</button>
                        </form>
                    )}
                </section>
            </div>
        </div>
    );
}
