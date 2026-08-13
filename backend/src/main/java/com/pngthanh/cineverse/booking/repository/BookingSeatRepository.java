package com.pngthanh.cineverse.booking.repository;
import com.pngthanh.cineverse.booking.entity.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> { List<BookingSeat> findAllByBookingId(Long bookingId); }
