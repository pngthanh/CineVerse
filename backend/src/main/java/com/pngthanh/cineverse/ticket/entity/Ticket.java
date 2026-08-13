package com.pngthanh.cineverse.ticket.entity;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.common.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = @Index(name = "idx_ticket_code", columnList = "ticket_code", unique = true))
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;
    @Column(name = "ticket_code", nullable = false, unique = true, length = 40)
    private String ticketCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.CONFIRMED;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
