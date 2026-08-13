package com.pngthanh.cineverse.booking.repository;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    Optional<Booking> findByBookingCode(String bookingCode);

    Optional<Booking> findByHoldToken(String holdToken);

    List<Booking> findAllByStatusAndExpiresAtBefore(BookingStatus status, Instant now);

    long countByStatus(BookingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
