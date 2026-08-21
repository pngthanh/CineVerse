import { FormEvent, useState } from 'react';
import { StatusBadge } from '../../components/StatusBadge';
import { ApiError, api } from '../../lib/api';
import { dateTime } from '../../lib/format';
import type { StaffTicketCheck } from '../../types';

export function StaffCheckInPage() {
    const [ticketCode, setTicketCode] = useState('');
    const [result, setResult] = useState<StaffTicketCheck | null>(null);
    const [error, setError] = useState('');
    const [working, setWorking] = useState(false);

    const lookup = async (event: FormEvent) => {
        event.preventDefault();
        setError('');
        setResult(null);
        try {
            setResult(await api<StaffTicketCheck>('/staff/tickets/lookup', {
                method: 'POST',
                body: JSON.stringify({ ticketCode }),
            }));
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể kiểm tra vé.');
        }
    };

    const scanImage = async (file?: File) => {
        if (!file) return;
        setError('');
        setResult(null);
        const data = new FormData();
        data.append('file', file);
        try {
            setResult(await api<StaffTicketCheck>('/staff/tickets/scan', { method: 'POST', body: data }));
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không đọc được QR trong ảnh.');
        }
    };

    const checkIn = async () => {
        if (!result) return;
        setWorking(true);
        setError('');
        try {
            const updated = await api<StaffTicketCheck>('/staff/tickets/check-in', {
                method: 'POST',
                body: JSON.stringify({ ticketCode: result.ticketCode }),
            });
            setResult(updated);
        } catch (requestError) {
            setError(requestError instanceof ApiError ? requestError.message : 'Không thể check-in vé.');
        } finally {
            setWorking(false);
        }
    };

    return (
        <div className="staff-page">
            <div className="page-title">
                <span className="eyebrow">CINEVERSE CHECK-IN</span>
                <h1>Kiểm tra vé</h1>
                <p>Nhập mã vé hoặc tải ảnh QR lên. Hệ thống tự kiểm tra đúng rạp, thanh toán và thời gian check-in.</p>
            </div>
            <div className="staff-check-grid">
                <section className="panel staff-check-tools">
                    <form onSubmit={lookup}>
                        <label>Mã vé
                            <div className="staff-code-row">
                                <input value={ticketCode} onChange={(event) => setTicketCode(event.target.value)} placeholder="CV-XXXXXXXXXX" required />
                                <button className="btn">Kiểm tra</button>
                            </div>
                        </label>
                    </form>
                    <div className="auth-divider"><span>hoặc</span></div>
                    <label className="staff-qr-upload">Upload ảnh QR vé
                        <input type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => void scanImage(event.target.files?.[0])} />
                        <span>Chọn ảnh QR từ máy</span>
                    </label>
                    {error && <div className="alert alert-error">{error}</div>}
                </section>

                <section className="panel staff-ticket-result">
                    {!result && <div className="empty"><h3>Chưa có vé được kiểm tra</h3><p>Kết quả xác minh sẽ hiển thị tại đây.</p></div>}
                    {result && <>
                        <div className="staff-ticket-head">
                            <div><small>{result.ticketCode}</small><h2>{result.movieTitle}</h2></div>
                            <StatusBadge value={result.ticketStatus} />
                        </div>
                        <div className={`staff-validation ${result.canCheckIn ? 'valid' : 'invalid'}`}>{result.validationMessage}</div>
                        <div className="details-list">
                            <div><dt>Rạp của vé</dt><dd>{result.cinemaName}</dd></div>
                            <div><dt>Rạp nhân viên</dt><dd>{result.staffCinemaName}</dd></div>
                            <div><dt>Phòng</dt><dd>{result.roomName}</dd></div>
                            <div><dt>Suất chiếu</dt><dd>{dateTime(result.startTime)}</dd></div>
                            <div><dt>Ghế</dt><dd>{result.seats.join(', ')}</dd></div>
                            <div><dt>Khách hàng</dt><dd>{result.customerName}</dd></div>
                            <div><dt>Booking</dt><dd><StatusBadge value={result.bookingStatus} /></dd></div>
                            <div><dt>Thanh toán</dt><dd><StatusBadge value={result.paymentStatus} /></dd></div>
                        </div>
                        {result.checkedInAt && <div className="alert alert-success">Đã check-in lúc {dateTime(result.checkedInAt)}{result.checkedInByName ? ` bởi ${result.checkedInByName}` : ''}.</div>}
                        {result.canCheckIn && <button className="btn btn-block" disabled={working} onClick={() => void checkIn()}>{working ? 'Đang xác nhận...' : 'Xác nhận check-in'}</button>}
                    </>}
                </section>
            </div>
        </div>
    );
}
