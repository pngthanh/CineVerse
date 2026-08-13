import { useEffect, useState } from 'react';
import { api } from '../../lib/api';
import type { UserProfile } from '../../types';
import { StatusBadge } from '../../components/StatusBadge';
export function AdminUsersPage() {
  const [items, setItems] = useState<UserProfile[]>([]);
  const load = () => api<UserProfile[]>('/admin/users').then(setItems);
  useEffect(() => {
    void load();
  }, []);
  const toggle = async (u: UserProfile) => {
    await api(`/admin/users/${u.id}/status?status=${u.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE'}`, {
      method: 'PATCH',
    });
    void load();
  };
  return (
    <div className="admin-page">
      <div className="page-title">
        <h1>Người dùng</h1>
        <p>Quản lý tài khoản. Mật khẩu không bao giờ được hiển thị.</p>
      </div>
      <section className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Họ tên</th>
              <th>Email</th>
              <th>Vai trò</th>
              <th>Trạng thái</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td>{u.role}</td>
                <td>
                  <StatusBadge value={u.status} />
                </td>
                <td>
                  <button className="btn btn-secondary btn-sm" onClick={() => toggle(u)}>
                    {u.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
