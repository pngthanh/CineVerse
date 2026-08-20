package com.pngthanh.cineverse.payment.entity;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(nullable = false, length = 40)
    private String provider = "VNPAY";
    @Column(length = 40)
    private String method = "VNPAY";
    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;
    @Column(name = "gateway_transaction_no", length = 100)
    private String gatewayTransactionNo;
    @Column(name = "bank_transaction_no", length = 255)
    private String bankTransactionNo;
    @Column(name = "bank_code", length = 30)
    private String bankCode;
    @Column(name = "card_type", length = 30)
    private String cardType;
    @Column(name = "response_code", length = 10)
    private String responseCode;
    @Column(name = "transaction_status", length = 10)
    private String transactionStatus;
    @Column(name = "callback_source", length = 20)
    private String callbackSource;
    private Instant paidAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public String getGatewayTransactionNo() { return gatewayTransactionNo; }
    public void setGatewayTransactionNo(String gatewayTransactionNo) { this.gatewayTransactionNo = gatewayTransactionNo; }
    public String getBankTransactionNo() { return bankTransactionNo; }
    public void setBankTransactionNo(String bankTransactionNo) { this.bankTransactionNo = bankTransactionNo; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }
    public String getCallbackSource() { return callbackSource; }
    public void setCallbackSource(String callbackSource) { this.callbackSource = callbackSource; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCreatedAt() { return createdAt; }
}
