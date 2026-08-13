package com.pngthanh.cineverse.booking.repository;

import com.pngthanh.cineverse.booking.entity.BookingConcession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingConcessionRepository extends JpaRepository<BookingConcession, Long> {
    List<BookingConcession> findAllByBookingIdOrderByIdAsc(Long bookingId);
}
