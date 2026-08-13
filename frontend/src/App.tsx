import { Route, Routes } from 'react-router-dom';
import { CustomerLayout } from './components/CustomerLayout';
import { AdminLayout } from './components/AdminLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { HomePage } from './pages/HomePage';
import { MoviesPage } from './pages/MoviesPage';
import { MovieDetailPage } from './pages/MovieDetailPage';
import { CinemasPage } from './pages/CinemasPage';
import { CinemaDetailPage } from './pages/CinemaDetailPage';
import { SelectShowtimePage } from './pages/SelectShowtimePage';
import { SeatSelectionPage } from './pages/SeatSelectionPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { PaymentPage } from './pages/PaymentPage';
import { BookingConfirmedPage, PaymentFailedPage } from './pages/BookingResultPages';
import { MyBookingsPage } from './pages/MyBookingsPage';
import { BookingDetailPage } from './pages/BookingDetailPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { AccountPage } from './pages/AccountPage';
import { AccessDeniedPage, NotFoundPage, SystemErrorPage } from './pages/ErrorPages';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminMoviesPage } from './pages/admin/AdminMoviesPage';
import { AdminCinemasPage } from './pages/admin/AdminCinemasPage';
import { AdminShowtimesPage } from './pages/admin/AdminShowtimesPage';
import { AdminBookingsPage } from './pages/admin/AdminBookingsPage';
import { AdminBookingDetailPage } from './pages/admin/AdminBookingDetailPage';
import { AdminUsersPage } from './pages/admin/AdminUsersPage';
export default function App() {
    return <Routes>
    <Route element={<CustomerLayout />}><Route path="/" element={<HomePage />}/><Route path="/movies" element={<MoviesPage />}/><Route path="/movies/:id" element={<MovieDetailPage />}/><Route path="/cinemas" element={<CinemasPage />}/><Route path="/cinemas/:id" element={<CinemaDetailPage />}/><Route path="/showtimes/select" element={<SelectShowtimePage />}/><Route element={<ProtectedRoute />}><Route path="/showtimes/:id/seats" element={<SeatSelectionPage />}/><Route path="/checkout" element={<CheckoutPage />}/><Route path="/payment" element={<PaymentPage />}/><Route path="/booking-confirmed" element={<BookingConfirmedPage />}/><Route path="/payment-failed" element={<PaymentFailedPage />}/><Route path="/bookings" element={<MyBookingsPage />}/><Route path="/bookings/:id" element={<BookingDetailPage />}/><Route path="/account" element={<AccountPage />}/></Route><Route path="/403" element={<AccessDeniedPage />}/><Route path="/500" element={<SystemErrorPage />}/></Route>
    <Route element={<CustomerLayout />}><Route path="/login" element={<LoginPage />}/><Route path="/register" element={<RegisterPage />}/></Route>
    <Route element={<ProtectedRoute admin/>}><Route element={<AdminLayout />}><Route path="/admin" element={<AdminDashboardPage />}/><Route path="/admin/movies" element={<AdminMoviesPage />}/><Route path="/admin/cinemas" element={<AdminCinemasPage />}/><Route path="/admin/showtimes" element={<AdminShowtimesPage />}/><Route path="/admin/bookings" element={<AdminBookingsPage />}/><Route path="/admin/bookings/:id" element={<AdminBookingDetailPage />}/><Route path="/admin/users" element={<AdminUsersPage />}/></Route></Route>
    <Route path="*" element={<NotFoundPage />}/></Routes>;
}

