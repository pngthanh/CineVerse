package com.pngthanh.cineverse.payment.controller;

import com.pngthanh.cineverse.payment.dto.MockPaymentRequest;
import com.pngthanh.cineverse.payment.dto.PaymentResponse;
import com.pngthanh.cineverse.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mock")
    public PaymentResponse mock(
            Authentication authentication,
            @Valid @RequestBody MockPaymentRequest request) {
        return paymentService.mock(authentication.getName(), request);
    }
}
