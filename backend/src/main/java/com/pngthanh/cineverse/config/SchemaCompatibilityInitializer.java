package com.pngthanh.cineverse.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaCompatibilityInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public SchemaCompatibilityInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        replaceCheck("users", "users_role_check", "role IN ('CUSTOMER','STAFF','ADMIN')");
        replaceCheck(
                "bookings",
                "bookings_status_check",
                "status IN ('PENDING','CONFIRMED','REFUND_PENDING','CANCELLED','COMPLETED')");
        replaceCheck(
                "payments",
                "payments_status_check",
                "status IN ('PENDING','SUCCESS','REFUND_PENDING','REFUNDED','REFUND_FAILED','FAILED')");
        replaceCheck(
                "movies",
                "movies_status_check",
                "status IN ('NOW_SHOWING','COMING_SOON','ENDED','INACTIVE')");
    }

    private void replaceCheck(String table, String name, String condition) {
        jdbc.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + name);
        jdbc.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " CHECK (" + condition + ")");
    }
}
