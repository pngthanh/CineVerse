const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

interface ApiErrorBody {
    code?: string;
    message?: string;
    fieldErrors?: Record<string, string>;
}

export class ApiError extends Error {
    constructor(
        public status: number,
        public code: string,
        message: string,
        public fieldErrors?: Record<string, string>,
    ) {
        super(message);
        this.name = 'ApiError';
    }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
    const token = localStorage.getItem('cineverse_token');
    const headers = new Headers(options.headers);

    if (!headers.has('Content-Type') && options.body) {
        headers.set('Content-Type', 'application/json');
    }
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    let response: Response;
    try {
        response = await fetch(`${API_URL}${path}`, { ...options, headers });
    } catch {
        throw new ApiError(
            0,
            'NETWORK_ERROR',
            'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.',
        );
    }

    if (!response.ok) {
        const body = await response.json().catch((): ApiErrorBody => ({
            code: 'HTTP_ERROR',
            message: 'Không thể xử lý yêu cầu.',
        })) as ApiErrorBody;

        throw new ApiError(
            response.status,
            body.code ?? 'HTTP_ERROR',
            body.message ?? 'Không thể xử lý yêu cầu.',
            body.fieldErrors,
        );
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}


export async function apiUploadImage(file: File): Promise<string> {
    const token = localStorage.getItem('cineverse_token');
    const body = new FormData();
    body.append('file', file);
    const headers = new Headers();
    if (token) headers.set('Authorization', `Bearer ${token}`);
    const response = await fetch(`${API_URL}/admin/media/images`, { method: 'POST', headers, body });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: 'Không thể tải ảnh lên.' }));
        throw new ApiError(response.status, error.code ?? 'UPLOAD_FAILED', error.message ?? 'Không thể tải ảnh lên.');
    }
    const data = await response.json() as { url: string };
    return data.url;
}
