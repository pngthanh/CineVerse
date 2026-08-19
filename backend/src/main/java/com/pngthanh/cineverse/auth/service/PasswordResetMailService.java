package com.pngthanh.cineverse.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendUrl;

    public PasswordResetMailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:${spring.mail.username:}}") String from,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public void sendResetLink(String recipient, String fullName, String rawToken) {
        String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(recipient);
        message.setSubject("CineVerse - Đặt lại mật khẩu");
        message.setText(
                "Xin chào " + fullName + ",\n\n"
                        + "Bạn vừa yêu cầu đặt lại mật khẩu CineVerse.\n"
                        + "Mở liên kết sau để tạo mật khẩu mới (hiệu lực 30 phút):\n"
                        + resetUrl + "\n\n"
                        + "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.\n\n"
                        + "CineVerse");
        mailSender.send(message);
    }
}
