import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { BookingSteps } from '../components/BookingSteps';
import { ApiError, api } from '../lib/api';
import { money, seatTypeLabel } from '../lib/format';
import type { SeatHold, SeatMap, SeatStatus } from '../types';

export function SeatSelectionPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [seatMap, setSeatMap] = useState<SeatMap | null>(null);
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([]);
  const [error, setError] = useState('');
  const [holding, setHolding] = useState(false);

  const loadSeatMap = async () => {
    if (id) setSeatMap(await api<SeatMap>(`/showtimes/${id}/seats`));
  };

  useEffect(() => {
    if (!id) return;
    let active = true;
    void api<SeatMap>(`/showtimes/${id}/seats`)
      .then((data) => {
        if (active) setSeatMap(data);
      })
      .catch(() => {
        if (active) setError('Không thể tải sơ đồ ghế.');
      });
    return () => {
      active = false;
    };
  }, [id]);

  const rows = useMemo(() => {
    const grouped: Record<string, SeatMap['seats']> = {};
    for (const seat of seatMap?.seats ?? []) {
      const row = seat.code.charAt(0);
      (grouped[row] ??= []).push(seat);
    }
    for (const seats of Object.values(grouped)) {
      seats.sort((a, b) => Number(a.code.slice(1)) - Number(b.code.slice(1)));
    }
    return Object.entries(grouped).sort(([a], [b]) => a.localeCompare(b));
  }, [seatMap]);

  const selectedSeats = useMemo(
    () => (seatMap?.seats ?? []).filter((seat) => selectedSeatIds.includes(seat.seatId)),
    [seatMap, selectedSeatIds],
  );
  const total = useMemo(
    () => selectedSeats.reduce((sum, seat) => sum + seat.price, 0),
    [selectedSeats],
  );

  const toggleSeat = (seatId: number, status: SeatStatus) => {
    if (status !== 'AVAILABLE') return;
    setSelectedSeatIds((current) =>
      current.includes(seatId) ? current.filter((value) => value !== seatId) : [...current, seatId],
    );
  };

  const holdSeats = async () => {
    if (!id || !selectedSeatIds.length) return;
    setHolding(true);
    setError('');
    try {
      const hold = await api<SeatHold>('/seat-holds', {
        method: 'POST',
        body: JSON.stringify({ showtimeId: Number(id), seatIds: selectedSeatIds }),
      });
      sessionStorage.setItem('cineverse_hold', JSON.stringify(hold));
      sessionStorage.setItem('cineverse_showtime_id', id);
      navigate('/checkout');
    } catch (exception) {
      setError(
        exception instanceof ApiError ? exception.message : 'Không thể giữ ghế. Vui lòng thử lại.',
      );
      setSelectedSeatIds([]);
      await loadSeatMap().catch(() => undefined);
    } finally {
      setHolding(false);
    }
  };

  const gridColumnForSeat = (seatNumber: number) =>
    seatNumber <= 5 ? seatNumber + 1 : seatNumber + 2;

  return (
    <div className="container page">
      <BookingSteps active={2} />
      <div className="page-title">
        <span className="eyebrow">SƠ ĐỒ PHÒNG CHIẾU</span>
        <h1>Chọn ghế</h1>
        <p>Chọn ghế phù hợp. Giá hiển thị được tính từ máy chủ.</p>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="seat-layout">
        <section className="seat-panel">
          <div className="screen">MÀN HÌNH</div>
          <div className="seat-legend">
            <span>□ Trống</span>
            <span className="legend-selected">■ Đang chọn</span>
            <span className="legend-held">■ Đang giữ</span>
            <span className="legend-booked">■ Đã đặt</span>
            <span className="legend-vip">▭ Khu vực VIP</span>
          </div>

          <div
            className="seat-map-scroll"
            aria-label="Sơ đồ ghế có thể cuộn ngang trên màn hình nhỏ"
          >
            <div className="seat-grid-stage">
              {rows.map(([row, seats], rowIndex) => (
                <div className="seat-grid-row" key={row} style={{ gridRow: rowIndex + 1 }}>
                  <strong className="seat-row-label">{row}</strong>
                  {seats.map((seat) => {
                    const seatNumber = Number(seat.code.slice(1));
                    return (
                      <button
                        key={seat.seatId}
                        style={{ gridColumn: gridColumnForSeat(seatNumber) }}
                        aria-label={`Ghế ${seat.code}, ${seat.type}, ${money(seat.price)}`}
                        aria-pressed={selectedSeatIds.includes(seat.seatId)}
                        disabled={seat.status !== 'AVAILABLE'}
                        onClick={() => toggleSeat(seat.seatId, seat.status)}
                        className={`seat seat-${seat.status.toLowerCase()} ${seat.type === 'VIP' ? 'seat-vip' : ''} ${selectedSeatIds.includes(seat.seatId) ? 'seat-selected' : ''}`}
                      >
                        <span>{seat.code}</span>
                        {seat.status === 'BOOKED' && (
                          <span className="seat-booked-x" aria-hidden="true">
                            ×
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
          <div className="aisle-legend">
            Khoảng trống giữa ghế 5 và 6 là lối đi trung tâm · VIP từ C3 đến F8
          </div>
        </section>

        <aside className="panel sticky selected-panel">
          <h3>Ghế đã chọn</h3>
          <div className="selected-list">
            {selectedSeats.length ? (
              selectedSeats.map((seat) => (
                <div className="selected-seat-row" key={seat.seatId}>
                  <span className="selected-seat-name">
                    {seat.code} · {seatTypeLabel(seat.type)}
                  </span>
                  <strong className="selected-seat-price">{money(seat.price)}</strong>
                </div>
              ))
            ) : (
              <p>Chưa chọn ghế.</p>
            )}
          </div>
          <div className="summary-row total">
            <span>Tạm tính</span>
            <strong>{money(total)}</strong>
          </div>
          <button
            disabled={!selectedSeatIds.length || holding}
            className="btn btn-block"
            onClick={holdSeats}
          >
            {holding ? 'Đang giữ ghế...' : 'Giữ ghế và tiếp tục'}
          </button>
        </aside>
      </div>
    </div>
  );
}
