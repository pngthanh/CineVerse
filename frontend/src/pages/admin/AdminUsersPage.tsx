import { useEffect, useState } from 'react';

import { StatusBadge } from '../../components/StatusBadge';
import { ApiError, api } from '../../lib/api';
import type { Cinema, Role, UserProfile } from '../../types';

export function AdminUsersPage() {
  const [items, setItems] = useState<UserProfile[]>([]);
  const [cinemas, setCinemas] = useState<Cinema[]>([]);
  const [workingId, setWorkingId] = useState<number | null>(null);
  const [pendingStaffIds, setPendingStaffIds] = useState<Set<number>>(new Set());
  const [error, setError] = useState('');

  const load = async () => {
    const [users, cinemaItems] = await Promise.all([
      api<UserProfile[]>('/admin/users'),
      api<Cinema[]>('/cinemas'),
    ]);

    setItems(users);
    setCinemas(cinemaItems);
  };

  useEffect(() => {
    let active = true;

    void Promise.all([api<UserProfile[]>('/admin/users'), api<Cinema[]>('/cinemas')]).then(
      ([users, cinemaItems]) => {
        if (!active) {
          return;
        }

        setItems(users);
        setCinemas(cinemaItems);
      },
    );

    return () => {
      active = false;
    };
  }, []);

  const toggle = async (user: UserProfile) => {
    setWorkingId(user.id);
    setError('');

    try {
      await api(
        `/admin/users/${user.id}/status?status=${user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE'}`,
        {
          method: 'PATCH',
        },
      );

      await load();
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
      } else {
        setError('Không thể cập nhật trạng thái tài khoản.');
      }
    } finally {
      setWorkingId(null);
    }
  };

  const updateAssignment = async (user: UserProfile, role: Role, cinemaId?: number) => {
    setWorkingId(user.id);
    setError('');

    try {
      await api(`/admin/users/${user.id}/staff-assignment`, {
        method: 'PATCH',
        body: JSON.stringify({
          role,
          cinemaId: role === 'STAFF' ? cinemaId : null,
        }),
      });

      setPendingStaffIds((current) => {
        const next = new Set(current);
        next.delete(user.id);
        return next;
      });

      await load();
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
      } else {
        setError('Không thể cập nhật phân công nhân viên.');
      }
    } finally {
      setWorkingId(null);
    }
  };

  const changeRole = (user: UserProfile, role: Role) => {
    setError('');

    if (role === 'STAFF') {
      /*
       * Chưa gọi backend ngay.
       *
       * Admin phải chọn rạp trước, sau đó frontend mới gửi:
       * role = STAFF + cinemaId.
       */
      setPendingStaffIds((current) => {
        const next = new Set(current);
        next.add(user.id);
        return next;
      });

      return;
    }

    setPendingStaffIds((current) => {
      const next = new Set(current);
      next.delete(user.id);
      return next;
    });

    void updateAssignment(user, 'CUSTOMER');
  };

  const isPendingStaff = (user: UserProfile) => pendingStaffIds.has(user.id);

  const displayedRole = (user: UserProfile): Role => (isPendingStaff(user) ? 'STAFF' : user.role);

  return (
    <div className="admin-page">
      <div className="page-title">
        <h1>Người dùng & nhân viên</h1>
        <p>Khóa tài khoản, cấp vai trò nhân viên và phân công nhân viên theo từng rạp.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Họ tên</th>
              <th>Email</th>
              <th>Vai trò</th>
              <th>Rạp phân công</th>
              <th>Trạng thái</th>
              <th></th>
            </tr>
          </thead>

          <tbody>
            {items.map((user) => {
              const role = displayedRole(user);

              return (
                <tr key={user.id}>
                  <td>
                    <strong>{user.fullName}</strong>
                    <br />
                    <small>{user.username ?? 'Không có username'}</small>
                  </td>

                  <td>{user.email ?? '—'}</td>

                  <td>
                    {user.role === 'ADMIN' ? (
                      <strong>ADMIN</strong>
                    ) : (
                      <select
                        value={role}
                        disabled={workingId === user.id}
                        onChange={(event) => changeRole(user, event.target.value as Role)}
                      >
                        <option value="CUSTOMER">CUSTOMER</option>
                        <option value="STAFF">STAFF</option>
                      </select>
                    )}
                  </td>

                  <td>
                    {role === 'STAFF' ? (
                      <select
                        value={isPendingStaff(user) ? '' : (user.assignedCinemaId ?? '')}
                        disabled={workingId === user.id}
                        onChange={(event) => {
                          const cinemaId = Number(event.target.value);

                          if (!cinemaId) {
                            return;
                          }

                          void updateAssignment(user, 'STAFF', cinemaId);
                        }}
                      >
                        <option value="" disabled>
                          Chọn rạp
                        </option>

                        {cinemas
                          .filter((cinema) => cinema.active)
                          .map((cinema) => (
                            <option key={cinema.id} value={cinema.id}>
                              {cinema.name}
                            </option>
                          ))}
                      </select>
                    ) : (
                      <span>—</span>
                    )}
                  </td>

                  <td>
                    <StatusBadge value={user.status} />
                  </td>

                  <td>
                    <button
                      className="btn btn-secondary btn-sm"
                      disabled={workingId === user.id || user.role === 'ADMIN'}
                      onClick={() => void toggle(user)}
                    >
                      {user.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </div>
  );
}
