import { useEffect, useRef, useState } from 'react';

type GoogleCredentialResponse = { credential: string };
type GoogleApi = {
    accounts: {
        id: {
            initialize: (config: { client_id: string; callback: (response: GoogleCredentialResponse) => void }) => void;
            renderButton: (element: HTMLElement, options: Record<string, string | number>) => void;
        };
    };
};

declare global {
    interface Window {
        google?: GoogleApi;
    }
}

interface Props {
    onCredential: (credential: string) => void | Promise<void>;
    text?: 'signin_with' | 'continue_with';
}

const SCRIPT_ID = 'google-identity-services';

export function GoogleIdentityButton({ onCredential, text = 'continue_with' }: Props) {
    const containerRef = useRef<HTMLDivElement>(null);
    const [error, setError] = useState('');
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;

    useEffect(() => {
        if (!clientId || !containerRef.current) return;
        let active = true;

        const render = () => {
            if (!active || !window.google || !containerRef.current) return;
            containerRef.current.innerHTML = '';
            window.google.accounts.id.initialize({
                client_id: clientId,
                callback: (response) => void onCredential(response.credential),
            });
            window.google.accounts.id.renderButton(containerRef.current, {
                theme: 'filled_black',
                size: 'large',
                shape: 'rectangular',
                text,
                width: Math.min(containerRef.current.clientWidth || 360, 400),
                locale: 'vi',
            });
        };

        if (window.google) {
            render();
            return () => { active = false; };
        }

        let script = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
        if (!script) {
            script = document.createElement('script');
            script.id = SCRIPT_ID;
            script.src = 'https://accounts.google.com/gsi/client';
            script.async = true;
            script.defer = true;
            document.head.appendChild(script);
        }
        script.addEventListener('load', render);
        script.addEventListener('error', () => { if (active) setError('Không thể tải Google Sign-In.'); });
        return () => {
            active = false;
            script?.removeEventListener('load', render);
        };
    }, [clientId, onCredential, text]);

    if (!clientId) {
        return <div className="google-config-note">Chưa cấu hình Google Login cho môi trường này.</div>;
    }

    return <div className="google-signin-wrap">{error && <small className="field-error">{error}</small>}<div ref={containerRef} /></div>;
}
