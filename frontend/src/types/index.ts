export type Role = 'CUSTOMER' | 'STAFF' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'LOCKED';
export type MovieStatus = 'NOW_SHOWING' | 'COMING_SOON' | 'INACTIVE';
export type SeatType = 'NORMAL' | 'VIP' | 'COUPLE';
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'BOOKED';
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type TicketStatus = 'CONFIRMED' | 'USED' | 'CANCELLED';

export interface UserProfile {
    id: number;
    fullName: string;
    email?: string;
    username?: string;
    localCredentials?: boolean;
    googleLinked?: boolean;
    googleEmail?: string;
    phone?: string;
    provinceCode?: string;
    provinceName?: string;
    districtCode?: string;
    districtName?: string;
    wardCode?: string;
    wardName?: string;
    addressDetail?: string;
    role: Role;
    assignedCinemaId?: number;
    assignedCinemaName?: string;
    status: UserStatus;
    createdAt: string;
}

export interface Movie {
    id: number;
    title: string;
    description: string;
    genres: string;
    durationMinutes: number;
    releaseDate?: string;
    endDate?: string;
    director?: string;
    castNames?: string;
    ageRating?: string;
    posterUrl?: string;
    backdropUrl?: string;
    trailerUrl?: string;
    status: MovieStatus;
    ticketsSold?: number;
}

export interface CinemaRoomSummary {
    id: number;
    name: string;
    active: boolean;
    rows: number;
    seatsPerRow: number;
    seatCount: number;
    vipSeatCount: number;
    weekdayBasePrice: number;
    weekendBasePrice: number;
    vipSurcharge: number;
}

export interface Cinema {
    id: number;
    name: string;
    address: string;
    active: boolean;
    rooms: CinemaRoomSummary[];
}

export interface Showtime {
    id: number;
    movieId: number;
    movieTitle: string;
    cinemaId: number;
    cinemaName: string;
    roomId: number;
    roomName: string;
    startTime: string;
    endTime: string;
    salesCloseTime: string;
    basePrice: number;
    active: boolean;
    lifecycleStatus: 'UPCOMING' | 'NOW_PLAYING' | 'ENDED' | 'CANCELLED';
    bookable: boolean;
}

export interface SeatItem {
    seatId: number;
    code: string;
    type: SeatType;
    status: SeatStatus;
    price: number;
    holdExpiresAt?: string;
}

export interface SeatMap {
    showtimeId: number;
    seats: SeatItem[];
}

export interface SeatHold {
    holdToken: string;
    expiresAt: string;
    seats: Array<{
        seatId: number;
        code: string;
        type: SeatType;
        price: number;
    }>;
    total: number;
}

export interface Booking {
    id: number;
    bookingCode: string;
    status: BookingStatus;
    createdAt: string;
    expiresAt: string;
    seatAmount: number;
    concessionAmount: number;
    subtotalAmount: number;
    discountAmount: number;
    totalAmount: number;
    voucherCode?: string;
    user?: {
        id: number;
        fullName: string;
        email: string;
        role: Role;
        status: UserStatus;
        createdAt: string;
    };
    showtime: {
        id: number;
        movieTitle: string;
        cinemaName: string;
        roomName: string;
        startTime: string;
    };
    seats: Array<{
        id: number;
        code: string;
        type: SeatType;
        price: number;
    }>;
    concessions: Array<{
        itemId: number;
        name: string;
        quantity: number;
        unitPrice: number;
        totalPrice: number;
    }>;
    paymentStatus?: PaymentStatus;
    paymentProvider?: string;
    paymentMethod?: string;
    paymentTransactionReference?: string;
    paymentTransactionNo?: string;
    paymentBankCode?: string;
    paymentCardType?: string;
    paymentResponseCode?: string;
    paymentPaidAt?: string;
    ticketCode?: string;
    ticketStatus?: TicketStatus;
    ticketCheckedInAt?: string;
    ticketCheckedInByName?: string;
}


export interface Voucher {
    id: number;
    code: string;
    title: string;
    description?: string;
    discountType: 'PERCENT' | 'FIXED';
    discountValue: number;
    minOrderAmount: number;
    maxDiscountAmount?: number;
    startsAt: string;
    expiresAt: string;
    active: boolean;
    publicVisible: boolean;
    audience: 'ALL' | 'SELECTED_USERS';
    movieId?: number;
    movieTitle?: string;
    usageLimit?: number;
    perUserLimit?: number;
    assignedUserIds: number[];
    saved: boolean;
    eligible: boolean;
}

export interface VoucherQuote {
    code: string;
    discountType: 'PERCENT' | 'FIXED';
    discountValue: number;
    subtotal: number;
    discountAmount: number;
    totalAmount: number;
}

export interface ConcessionItem {
    id: number;
    name: string;
    description: string;
    price: number;
}

export interface AdminConcessionItem extends ConcessionItem {
    active: boolean;
}

export interface StaffTicketCheck {
    ticketCode: string;
    ticketStatus: TicketStatus;
    bookingStatus: BookingStatus;
    paymentStatus?: PaymentStatus;
    movieTitle: string;
    cinemaId: number;
    cinemaName: string;
    roomName: string;
    startTime: string;
    seats: string[];
    customerName: string;
    staffCinemaId: number;
    staffCinemaName: string;
    sameCinema: boolean;
    canCheckIn: boolean;
    validationMessage: string;
    checkedInAt?: string;
    checkedInByName?: string;
}
