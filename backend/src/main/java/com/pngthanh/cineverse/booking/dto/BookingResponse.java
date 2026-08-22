package com.pngthanh.cineverse.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingCode,
        String status,
        Instant createdAt,
        Instant expiresAt,
        BigDecimal seatAmount,
        BigDecimal concessionAmount,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String voucherCode,
        String cancellationReason,
        Instant refundRequestedAt,
        UserInfo user,
        ShowtimeInfo showtime,
        List<SeatInfo> seats,
        List<ConcessionInfo> concessions,
        String paymentStatus,
        String paymentProvider,
        String paymentMethod,
        String paymentTransactionReference,
        String paymentTransactionNo,
        String paymentBankCode,
        String paymentCardType,
        String paymentResponseCode,
        Instant paymentPaidAt,
        String refundRequestId,
        String refundResponseCode,
        String refundTransactionStatus,
        String refundTransactionNo,
        String refundMessage,
        Instant refundCompletedAt,
        String ticketCode,
        String ticketStatus,
        Instant ticketCheckedInAt,
        String ticketCheckedInByName) {


    public record UserInfo(
            Long id,
            String fullName,
            String email,
            String role,
            String status,
            Instant createdAt) {
    }

    public record ShowtimeInfo(
            Long id,
            String movieTitle,
            String cinemaName,
            String roomName,
            LocalDateTime startTime) {
    }

    public record SeatInfo(
            Long id,
            String code,
            String type,
            BigDecimal price) {
    }

    public record ConcessionInfo(
            Long itemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice) {
    }
}
