package com.pngthanh.cineverse.voucher.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(-100)
public class VoucherDataMigration implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public VoucherDataMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        jdbc.update("update vouchers set title = concat('Ưu đãi ', code) where title is null");
        jdbc.update("update vouchers set discount_type = 'PERCENT' where discount_type is null");
        jdbc.update("update vouchers set discount_value = discount_percent where discount_value is null");
        jdbc.update("update vouchers set audience = 'ALL' where audience is null");
        jdbc.update("update vouchers set public_visible = true where public_visible is null");
    }
}
