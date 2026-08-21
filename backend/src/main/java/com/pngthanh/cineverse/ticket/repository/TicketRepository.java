package com.pngthanh.cineverse.ticket.repository;

import com.pngthanh.cineverse.ticket.entity.Ticket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByBookingId(Long bookingId);

    Optional<Ticket> findByTicketCodeIgnoreCase(String ticketCode);

    Optional<Ticket> findByQrToken(String qrToken);
}
