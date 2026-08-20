package com.pngthanh.cineverse.payment.controller;

import com.pngthanh.cineverse.payment.dto.VnPayCreatePaymentRequest;
import com.pngthanh.cineverse.payment.dto.VnPayCreatePaymentResponse;
import com.pngthanh.cineverse.payment.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/vnpay")
public class PaymentController {
    private final VnPayService vnPayService;
    private final String frontendUrl;

    public PaymentController(
            VnPayService vnPayService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.vnPayService = vnPayService;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/create")
    public VnPayCreatePaymentResponse create(
            Authentication authentication,
            @Valid @RequestBody VnPayCreatePaymentRequest request,
            HttpServletRequest servletRequest) {
        return vnPayService.createPayment(
                authentication.getName(),
                request.bookingId(),
                servletRequest);
    }

    @GetMapping("/return")
    public ResponseEntity<Void> returnUrl(@RequestParam Map<String, String> params) {
        VnPayService.CallbackResult result = vnPayService.processCallback(params, "RETURN");
        String destination;
        if (!result.valid()) {
            destination = frontendUrl + "/payment-failed?reason=invalid-callback";
        } else if (result.success()) {
            destination = frontendUrl + "/booking-confirmed?id=" + result.bookingId();
        } else {
            destination = frontendUrl + "/payment-failed?id=" + result.bookingId()
                    + "&code=" + safe(result.responseCode());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(destination));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        VnPayService.CallbackResult result = vnPayService.processCallback(params, "IPN");
        return Map.of("RspCode", result.rspCode(), "Message", result.message());
    }

    private String safe(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.chars()
                .filter(Character::isLetterOrDigit)
                .mapToObj(character -> String.valueOf((char) character))
                .collect(Collectors.joining());
    }
}
