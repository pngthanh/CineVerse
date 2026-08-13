import { useEffect, useState } from 'react';
import { api } from '../../lib/api';
interface Stats {
  totalUsers: number;
  totalBookings: number;
  confirmedBookings: number;
  totalMovies: number;
  totalCinemas: number;
}
export function AdminDashboardPage() {
  const [s, setS] = useState<Stats | null>(null);
  useEffect(() => {
    void api<Stats>('/admin/dashboard').then(setS);
  }, []);
  return (
    <div className="admin-page">
      <div className="page-title">
        <h1>Tổng quan</h1>
        <p>Tình trạng vận hành CineVerse.</p>
      </div>
      <div className="stats-grid">
        <div>
          <span>Người dùng</span>
          <strong>{s?.totalUsers ?? 0}</strong>
        </div>
        <div>
          <span>Đặt vé</span>
          <strong>{s?.totalBookings ?? 0}</strong>
        </div>
        <div>
          <span>Đã xác nhận</span>
          <strong>{s?.confirmedBookings ?? 0}</strong>
        </div>
        <div>
          <span>Phim</span>
          <strong>{s?.totalMovies ?? 0}</strong>
        </div>
        <div>
          <span>Rạp</span>
          <strong>{s?.totalCinemas ?? 0}</strong>
        </div>
      </div>
      <div className="panel">
        <h3>Ghi chú V1</h3>
        <p>
          Dashboard ưu tiên số liệu lõi. Các biểu đồ nâng cao được để roadmap sau khi booking flow
          ổn định.
        </p>
      </div>
    </div>
  );
}
