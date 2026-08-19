import { ReactNode, useEffect } from 'react';

interface ModalProps {
    open: boolean;
    title: string;
    children: ReactNode;
    onClose: () => void;
    wide?: boolean;
}

export function Modal({ open, title, children, onClose, wide = false }: ModalProps) {
    useEffect(() => {
        if (!open) return;
        const closeOnEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') onClose();
        };
        document.addEventListener('keydown', closeOnEscape);
        document.body.classList.add('modal-open');
        return () => {
            document.removeEventListener('keydown', closeOnEscape);
            document.body.classList.remove('modal-open');
        };
    }, [open, onClose]);

    if (!open) return null;

    return (
        <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
            <section
                className={`app-modal ${wide ? 'app-modal-wide' : ''}`}
                role="dialog"
                aria-modal="true"
                aria-label={title}
                onMouseDown={(event) => event.stopPropagation()}
            >
                <div className="app-modal-head">
                    <h2>{title}</h2>
                    <button className="modal-close" type="button" onClick={onClose} aria-label="Đóng">×</button>
                </div>
                <div className="app-modal-body">{children}</div>
            </section>
        </div>
    );
}
