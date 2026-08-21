import { apiBlob } from '../lib/api';
import { useEffect, useState } from 'react';

export function TicketQrCard({ ticketCode }: { ticketCode: string }) {
    const [src, setSrc] = useState('');
    const [error, setError] = useState(false);

    useEffect(() => {
        let active = true;
        let objectUrl = '';
        void apiBlob(`/tickets/${ticketCode}/qr`)
            .then((blob) => {
                objectUrl = URL.createObjectURL(blob);
                if (active) setSrc(objectUrl);
            })
            .catch(() => { if (active) setError(true); });
        return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl); };
    }, [ticketCode]);

    const download = async () => {
        const blob = await apiBlob(`/tickets/${ticketCode}/download`);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `CineVerse-${ticketCode}.png`;
        anchor.click();
        URL.revokeObjectURL(url);
    };

    if (error) return <div className="alert alert-error">Không thể tải QR vé.</div>;
    return <div className="ticket-qr-card">{src ? <img src={src} alt={`QR vé ${ticketCode}`} /> : <div className="ticket-qr-loading">Đang tạo QR...</div>}<div><strong>QR check-in</strong><p>Xuất trình QR tại đúng rạp. Nhân viên có thể upload ảnh QR để xác minh.</p><button className="btn btn-secondary btn-sm" onClick={() => void download()}>Tải vé PNG</button></div></div>;
}
