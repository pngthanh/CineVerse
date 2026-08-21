package com.pngthanh.cineverse.booking.entity;

import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.user.entity.User;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bookings", indexes = @Index(name = "idx_bookings_code", columnList = "booking_code", unique = true))
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "booking_code", nullable = false, unique = true, length = 40)
    private String bookingCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "showtime_id")
    private Showtime showtime;
    @Column(nullable = false, unique = true, length = 64)
    private String holdToken;
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;
    @Column(precision = 12, scale = 0)
    private BigDecimal subtotalAmount;
    @Column(precision = 12, scale = 0)
    private BigDecimal concessionAmount;
    @Column(precision = 12, scale = 0)
    private BigDecimal discountAmount;
    @Column(length = 30)
    private String voucherCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
    @Column(name = "refund_requested_at")
    private Instant refundRequestedAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }
    public String getHoldToken() { return holdToken; }
    public void setHoldToken(String holdToken) { this.holdToken = holdToken; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public BigDecimal getConcessionAmount() { return concessionAmount; }
    public void setConcessionAmount(BigDecimal concessionAmount) { this.concessionAmount = concessionAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public Instant getRefundRequestedAt() { return refundRequestedAt; }
    public void setRefundRequestedAt(Instant refundRequestedAt) { this.refundRequestedAt = refundRequestedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
