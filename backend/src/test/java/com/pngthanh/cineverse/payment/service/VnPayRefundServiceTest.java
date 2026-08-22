package com.pngthanh.cineverse.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import tools.jackson.databind.ObjectMapper;

import com.pngthanh.cineverse.booking.entity.Booking;
import com.pngthanh.cineverse.booking.service.BookingService;
import com.pngthanh.cineverse.common.enums.BookingStatus;
import com.pngthanh.cineverse.common.enums.PaymentStatus;
import com.pngthanh.cineverse.payment.dto.VnPayRefundResponse;
import com.pngthanh.cineverse.payment.entity.Payment;
import com.pngthanh.cineverse.payment.repository.PaymentRepository;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class VnPayRefundServiceTest {

    @Test
    void successfulRefundCancelsBookingAndMarksPaymentRefunded() {

        PaymentRepository payments =
                mock(PaymentRepository.class);

        BookingService bookings =
                mock(BookingService.class);

        VnPayConfig config =
                mock(VnPayConfig.class);

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        RestClient.Builder restClientBuilder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(restClientBuilder)
                        .build();

        VnPayRefundService service =
                new VnPayRefundService(
                        payments,
                        bookings,
                        config,
                        new ObjectMapper(),
                        restClientBuilder);

        Booking booking = new Booking();

        booking.setBookingCode("CV-REFUND-1");
        booking.setStatus(BookingStatus.REFUND_PENDING);

        Payment payment = new Payment();

        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("180000"));
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        payment.setTransactionReference("CV1-TEST");
        payment.setGatewayTransactionNo("14901234");
        payment.setTransactionCreatedAt(
                Instant.parse("2026-08-21T08:00:00Z"));

        when(bookings.requireForUpdate(1L))
                .thenReturn(booking);

        when(payments.findByBookingId(1L))
                .thenReturn(Optional.of(payment));

        when(config.isConfigured())
                .thenReturn(true);

        when(config.getTmnCode())
                .thenReturn("TESTCODE");

        when(config.getHashSecret())
                .thenReturn("test-secret");

        when(config.getApiUrl())
                .thenReturn("http://localhost/vnpay-refund");

        when(request.getHeader("X-Forwarded-For"))
                .thenReturn(null);

        when(request.getRemoteAddr())
                .thenReturn("127.0.0.1");

        server.expect(
                        requestTo(
                                "http://localhost/vnpay-refund"))
                .andRespond(
                        withSuccess(
                                "{\"vnp_ResponseCode\":\"00\","
                                        + "\"vnp_TransactionStatus\":\"00\","
                                        + "\"vnp_TransactionNo\":\"RF123\","
                                        + "\"vnp_Message\":\"Success\"}",
                                MediaType.APPLICATION_JSON));

        VnPayRefundResponse response =
                service.refund(
                        1L,
                        "admin@cineverse.vn",
                        request);

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus());

        assertEquals(
                PaymentStatus.REFUNDED,
                payment.getStatus());

        assertEquals(
                PaymentStatus.REFUNDED.name(),
                response.paymentStatus());

        assertEquals(
                "RF123",
                response.refundTransactionNo());

        server.verify();
    }

    @Test
    void rejectedRefundRemainsRetryable() {

        PaymentRepository payments =
                mock(PaymentRepository.class);

        BookingService bookings =
                mock(BookingService.class);

        VnPayConfig config =
                mock(VnPayConfig.class);

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        RestClient.Builder restClientBuilder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(restClientBuilder)
                        .build();

        VnPayRefundService service =
                new VnPayRefundService(
                        payments,
                        bookings,
                        config,
                        new ObjectMapper(),
                        restClientBuilder);

        Booking booking = new Booking();

        booking.setBookingCode("CV-REFUND-2");
        booking.setStatus(BookingStatus.REFUND_PENDING);

        Payment payment = new Payment();

        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("150000"));
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        payment.setTransactionReference("CV2-TEST");
        payment.setGatewayTransactionNo("14909999");
        payment.setTransactionCreatedAt(
                Instant.parse("2026-08-21T08:30:00Z"));

        when(bookings.requireForUpdate(2L))
                .thenReturn(booking);

        when(payments.findByBookingId(2L))
                .thenReturn(Optional.of(payment));

        when(config.isConfigured())
                .thenReturn(true);

        when(config.getTmnCode())
                .thenReturn("TESTCODE");

        when(config.getHashSecret())
                .thenReturn("test-secret");

        when(config.getApiUrl())
                .thenReturn("http://localhost/vnpay-refund");

        when(request.getHeader("X-Forwarded-For"))
                .thenReturn(null);

        when(request.getRemoteAddr())
                .thenReturn("127.0.0.1");

        server.expect(
                        requestTo(
                                "http://localhost/vnpay-refund"))
                .andRespond(
                        withSuccess(
                                "{\"vnp_ResponseCode\":\"91\","
                                        + "\"vnp_TransactionStatus\":\"91\","
                                        + "\"vnp_Message\":\"Rejected\"}",
                                MediaType.APPLICATION_JSON));

        VnPayRefundResponse response =
                service.refund(
                        2L,
                        "admin@cineverse.vn",
                        request);

        assertEquals(
                BookingStatus.REFUND_PENDING,
                booking.getStatus());

        assertEquals(
                PaymentStatus.REFUND_FAILED,
                payment.getStatus());

        assertEquals(
                PaymentStatus.REFUND_FAILED.name(),
                response.paymentStatus());

        server.verify();
    }
}