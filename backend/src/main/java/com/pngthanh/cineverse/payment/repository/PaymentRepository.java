package com.pngthanh.cineverse.payment.repository;
import com.pngthanh.cineverse.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment, Long> { Optional<Payment> findByBookingId(Long bookingId); }
