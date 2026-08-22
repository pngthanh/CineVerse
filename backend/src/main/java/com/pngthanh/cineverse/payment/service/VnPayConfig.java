package com.pngthanh.cineverse.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VnPayConfig {
    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final String apiUrl;

    public VnPayConfig(
            @Value("${app.vnpay.tmn-code:}") String tmnCode,
            @Value("${app.vnpay.hash-secret:}") String hashSecret,
            @Value("${app.vnpay.pay-url}") String payUrl,
            @Value("${app.vnpay.return-url}") String returnUrl,
            @Value("${app.vnpay.api-url}") String apiUrl) {
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.payUrl = payUrl;
        this.returnUrl = returnUrl;
        this.apiUrl = apiUrl;
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public boolean isConfigured() {
        return !tmnCode.isBlank() && !hashSecret.isBlank();
    }
}
