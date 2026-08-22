package com.pngthanh.cineverse.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdvancedDashboardResponse(
        long bookings,
        long ticketsSold,
        long checkedInTickets,
        BigDecimal seatRevenue,
        BigDecimal concessionRevenue,
        BigDecimal discountAmount,
        BigDecimal netRevenue,
        List<TrendPoint> trend,
        List<CinemaRow> cinemas,
        List<MovieRow> movies,
        List<ConcessionRow> concessions,
        List<VoucherRow> vouchers,
        List<PaymentMethodRow> paymentMethods) {

    public record TrendPoint(String label, BigDecimal revenue, long tickets) {
    }

    public record CinemaRow(Long id, String name, long bookings, long tickets, BigDecimal revenue) {
    }

    public record MovieRow(Long id, String title, String posterUrl, long bookings, long tickets, BigDecimal revenue) {
    }

    public record ConcessionRow(Long id, String name, long quantity, BigDecimal revenue) {
    }

    public record VoucherRow(String code, long uses, BigDecimal discountAmount) {
    }

    public record PaymentMethodRow(String method, long transactions, BigDecimal revenue) {
    }
}
