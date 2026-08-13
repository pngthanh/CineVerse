import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import type { Cinema } from '../types';
export function CinemasPage() {
  const [items, setItems] = useState<Cinema[]>([]);
  useEffect(() => {
    void api<Cinema[]>('/cinemas').then(setItems);
  }, []);
  return (
    <div className="container page">
      <div className="page-title">
        <h1>Rạp CineVerse</h1>
        <p>Chọn rạp và xem lịch chiếu phù hợp.</p>
      </div>
      <div className="cinema-grid">
        {items.map((c) => (
          <Link to={`/cinemas/${c.id}`} className="cinema-card" key={c.id}>
            <span>CINEVERSE</span>
            <h3>{c.name}</h3>
            <p>{c.address}</p>
            <small>{c.rooms.length} phòng chiếu</small>
          </Link>
        ))}
      </div>
    </div>
  );
}
