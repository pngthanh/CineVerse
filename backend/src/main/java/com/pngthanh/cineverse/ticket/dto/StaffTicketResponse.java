package com.pngthanh.cineverse.ticket.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record StaffTicketResponse(
        String ticketCode,
        String ticketStatus,
        String bookingStatus,
        String paymentStatus,
        String movieTitle,
        Long cinemaId,
        String cinemaName,
        String roomName,
        LocalDateTime startTime,
        List<String> seats,
        String customerName,
        Long staffCinemaId,
        String staffCinemaName,
        boolean sameCinema,
        boolean canCheckIn,
        String validationMessage,
        Instant checkedInAt,
        String checkedInByName) {
}
