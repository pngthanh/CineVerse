import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookingSteps } from '../components/BookingSteps';
import { useCountdown } from '../hooks/useCountdown';
import { ApiError, api } from '../lib/api';
import { money, seatTypeLabel } from '../lib/format';
import type { Booking, ConcessionItem, SeatHold, VoucherQuote } from '../types';

export function CheckoutPage() {
    const navigate = useNavigate();
    const hold = useMemo(() => {
        try {
            return JSON.parse(sessionStorage.getItem('cineverse_hold') || 'null') as SeatHold | null;
        }
        catch {
            return null;
        }
    }, []);
    const countdown = useCountdown(hold?.expiresAt);
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [voucherCode, setVoucherCode] = useState('');
    const [voucherQuote, setVoucherQuote] = useState<VoucherQuote | null>(null);
    const [checkingVoucher, setCheckingVoucher] = useState(false);
    const [concessions, setConcessions] = useState<ConcessionItem[]>([]);
    const [concessionQuantities, setConcessionQuantities] = useState<Record<number, number>>({});

    useEffect(() => {
        let active = true;
        void api<ConcessionItem[]>('/concessions')
            .then((items) => {
                if (active) setConcessions(items);
            })
            .catch(() => {
                if (active) setError('Không thể tải danh sách bắp nước.');
            });
        return () => {
            active = false;
        };
    }, []);

    const concessionTotal = useMemo(
        () => concessions.reduce(
            (sum, item) => sum + item.price * (concessionQuantities[item.id] ?? 0),
            0,
        ),
        [concessions, concessionQuantities],
    );
    const subtotal = (hold?.total ?? 0) + concessionTotal;

    const updateConcession = (itemId: number, delta: number) => {
        setConcessionQuantities((current) => {
            const nextQuantity = Math.max(0, Math.min(10, (current[itemId] ?? 0) + delta));
            return { ...current, [itemId]: nextQuantity };
        });
        setVoucherQuote(null);
    };

    const createBooking = async () => {
        if (!hold || countdown.expired) return;
        setSubmitting(true);
        setError('');
        try {
            const selectedConcessions = concessions
                .map((item) => ({ itemId: item.id, quantity: concessionQuantities[item.id] ?? 0 }))
                .filter((item) => item.quantity > 0);
            const booking = await api<Booking>('/bookings', {
                method: 'POST',
                body: JSON.stringify({
                    holdToken: hold.holdToken,
                    voucherCode: voucherQuote?.code,
                    concessions: selectedConcessions,
                }),
            });
            sessionStorage.setItem('cineverse_booking', JSON.stringify(booking));
            sessionStorage.removeItem('cineverse_hold');
            navigate('/payment');
        }
        catch (requestError) {
            setError(requestError instanceof ApiError
                ? requestError.message
                : 'Không thể tạo booking. Vui lòng thử lại.');
        }
        finally {
            setSubmitting(false);
        }
    };

    const applyVoucher = async () => {
        if (!hold || !voucherCode.trim()) return;
        setCheckingVoucher(true);
        setError('');
        try {
            const quote = await api<VoucherQuote>('/vouchers/quote', {
                method: 'POST',
                body: JSON.stringify({ code: voucherCode.trim(), subtotal }),
            });
            setVoucherQuote(quote);
            setVoucherCode(quote.code);
        }
        catch (requestError) {
            setVoucherQuote(null);
            setError(requestError instanceof ApiError
                ? requestError.message
                : 'Không thể kiểm tra mã ưu đãi.');
        }
        finally {
            setCheckingVoucher(false);
        }
    };

    const removeVoucher = () => {
        setVoucherQuote(null);
        setVoucherCode('');
        setError('');
    };

    if (!hold) {
        return (
            <div className="page-center">
                <div className="panel">
                    <h2>Không có phiên giữ ghế</h2>
                    <button className="btn" onClick={() => navigate('/movies')}>Chọn phim</button>
                </div>
            </div>
        );
    }

    if (countdown.expired) {
        return (
            <div className="page-center">
                <div className="panel">
                    <h2>Phiên giữ ghế đã hết hạn</h2>
                    <p>Ghế đã được trả lại để người khác có thể chọn.</p>
                    <button
                        className="btn"
                        onClick={() => navigate(`/showtimes/${sessionStorage.getItem('cineverse_showtime_id')}/seats`)}
                    >
                        Chọn ghế lại
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="container page">
            <BookingSteps active={3} />
            <div className="page-title">
                <h1>Xác nhận đặt vé</h1>
                <p>Kiểm tra ghế, chọn bắp nước và xác nhận số tiền trước khi tiếp tục.</p>
            </div>

            <div className="hold-timer" role="status">
                Ghế đang được giữ trong <strong>{countdown.label}</strong>
            </div>
            <p className="resume-hint">
                Nếu bạn rời trang lúc này, trong thời gian ghế còn được giữ hãy bấm
                <strong> “Đang giữ ghế · Tiếp tục”</strong> trên header để quay lại bước xác nhận.
            </p>

            {error && <div className="alert alert-error">{error}</div>}

            <div className="two-col">
                <div className="checkout-content">
                    <section className="panel">
                        <h3>Ghế đã giữ</h3>
                        {hold.seats.map((seat) => (
                            <div className="summary-row" key={seat.seatId}>
                                <span>{seat.code} · {seatTypeLabel(seat.type)}</span>
                                <strong>{money(seat.price)}</strong>
                            </div>
                        ))}
                    </section>

                    <section className="panel concession-panel">
                        <div className="concession-heading">
                            <div>
                                <span className="eyebrow">BẮP NƯỚC</span>
                                <h3>Thêm món cho suất chiếu</h3>
                            </div>
                            <strong>{money(concessionTotal)}</strong>
                        </div>
                        <div className="concession-list">
                            {concessions.map((item) => {
                                const quantity = concessionQuantities[item.id] ?? 0;
                                return (
                                    <article className={`concession-item ${quantity ? 'selected' : ''}`} key={item.id}>
                                        <div className="concession-copy">
                                            <strong>{item.name}</strong>
                                            <p>{item.description}</p>
                                            <span>{money(item.price)}</span>
                                        </div>
                                        <div className="quantity-control" aria-label={`Số lượng ${item.name}`}>
                                            <button
                                                type="button"
                                                disabled={!quantity}
                                                onClick={() => updateConcession(item.id, -1)}
                                                aria-label={`Giảm ${item.name}`}
                                            >−</button>
                                            <strong>{quantity}</strong>
                                            <button
                                                type="button"
                                                disabled={quantity >= 10}
                                                onClick={() => updateConcession(item.id, 1)}
                                                aria-label={`Thêm ${item.name}`}
                                            >+</button>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                        <p className="concession-note">Giá bắp nước được backend kiểm tra lại khi tạo booking.</p>
                    </section>
                </div>

                <aside className="panel sticky">
                    <h3>Tổng thanh toán</h3>
                    <div className="voucher-box">
                        <label htmlFor="voucher-code">Mã ưu đãi</label>
                        <div className="voucher-form">
                            <input
                                id="voucher-code"
                                value={voucherCode}
                                disabled={Boolean(voucherQuote)}
                                placeholder="Ví dụ: CINE10"
                                onChange={(event) => setVoucherCode(event.target.value.toUpperCase())}
                            />
                            {voucherQuote ? (
                                <button className="btn btn-secondary btn-sm" type="button" onClick={removeVoucher}>
                                    Bỏ mã
                                </button>
                            ) : (
                                <button
                                    className="btn btn-secondary btn-sm"
                                    type="button"
                                    disabled={!voucherCode.trim() || checkingVoucher}
                                    onClick={applyVoucher}
                                >
                                    {checkingVoucher ? 'Đang kiểm tra...' : 'Áp dụng'}
                                </button>
                            )}
                        </div>
                        {voucherQuote && (
                            <p className="voucher-success">
                                Đã áp dụng {voucherQuote.code} · giảm {voucherQuote.discountPercent}%
                            </p>
                        )}
                    </div>
                    <div className="summary-row">
                        <span>Vé xem phim</span>
                        <strong>{money(hold.total)}</strong>
                    </div>
                    <div className="summary-row">
                        <span>Bắp nước</span>
                        <strong>{money(concessionTotal)}</strong>
                    </div>
                    <div className="summary-row">
                        <span>Tạm tính</span>
                        <strong>{money(subtotal)}</strong>
                    </div>
                    {voucherQuote && (
                        <div className="summary-row voucher-discount">
                            <span>Giảm giá</span>
                            <strong>-{money(voucherQuote.discountAmount)}</strong>
                        </div>
                    )}
                    <div className="summary-row total">
                        <span>Tổng cộng</span>
                        <strong>{money(voucherQuote?.totalAmount ?? subtotal)}</strong>
                    </div>
                    <button className="btn btn-block" disabled={submitting} onClick={createBooking}>
                        {submitting ? 'Đang tạo booking...' : 'Tiếp tục thanh toán'}
                    </button>
                </aside>
            </div>
        </div>
    );
}
