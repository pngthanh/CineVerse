import { FormEvent, useEffect, useState } from 'react';
import { api } from '../lib/api';
import { roleLabel, statusLabel } from '../lib/format';
import type { UserProfile } from '../types';
export function AccountPage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [name, setName] = useState('');
  const [message, setMessage] = useState('');
  useEffect(() => {
    void api<UserProfile>('/me').then((p) => {
      setProfile(p);
      setName(p.fullName);
    });
  }, []);
  const save = async (e: FormEvent) => {
    e.preventDefault();
    const p = await api<UserProfile>('/me', {
      method: 'PATCH',
      body: JSON.stringify({ fullName: name }),
    });
    setProfile(p);
    setMessage('Đã cập nhật hồ sơ.');
  };
  return (
    <div className="container page">
      <div className="page-title">
        <h1>Tài khoản của tôi</h1>
        <p>Quản lý thông tin cá nhân CineVerse.</p>
      </div>
      <div className="two-col">
        <form className="panel" onSubmit={save}>
          <h3>Hồ sơ</h3>
          <label>
            Họ và tên
            <input value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <label>
            Email
            <input value={profile?.email ?? ''} disabled />
          </label>
          {message && <div className="alert alert-success">{message}</div>}
          <button className="btn">Lưu thay đổi</button>
        </form>
        <aside className="panel">
          <h3>Thông tin tài khoản</h3>
          <p>Vai trò: {roleLabel(profile?.role)}</p>
          <p>Trạng thái: {statusLabel(profile?.status)}</p>
        </aside>
      </div>
    </div>
  );
}
