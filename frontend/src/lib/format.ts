import type { SeatType } from '../types';

export const money = (value: number) => new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
}).format(value);

export const dateTime = (value: string) => new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
}).format(new Date(value));

export const timeOnly = (value: string) => new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
}).format(new Date(value));

const statusLabels: Record<string, string> = {
    ACTIVE: 'Đang hoạt động',
    LOCKED: 'Đã khóa',
    PENDING: 'Chờ xử lý',
    CONFIRMED: 'Đã xác nhận',
    CANCELLED: 'Đã hủy',
    COMPLETED: 'Hoàn thành',
    SUCCESS: 'Thành công',
    FAILED: 'Thất bại',
    USED: 'Đã sử dụng',
    AVAILABLE: 'Còn trống',
    HELD: 'Đang giữ',
    BOOKED: 'Đã đặt',
    NOW_SHOWING: 'Đang chiếu',
    COMING_SOON: 'Sắp chiếu',
    UPCOMING: 'Sắp chiếu',
    NOW_PLAYING: 'Đang chiếu',
    ENDED: 'Đã kết thúc',
    INACTIVE: 'Ngừng hoạt động',
};

const roleLabels: Record<string, string> = {
    CUSTOMER: 'Khách hàng',
    ADMIN: 'Quản trị viên',
};

const seatTypeLabels: Record<SeatType, string> = {
    NORMAL: 'Ghế thường',
    VIP: 'Ghế VIP',
    COUPLE: 'Ghế đôi',
};

export const statusLabel = (value?: string) => {
    if (!value) {
        return 'Chưa xác định';
    }
    return statusLabels[value] ?? value;
};

export const roleLabel = (value?: string) => {
    if (!value) {
        return 'Chưa xác định';
    }
    return roleLabels[value] ?? value;
};

export const seatTypeLabel = (value: SeatType) => seatTypeLabels[value];
