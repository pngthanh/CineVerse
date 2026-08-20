package com.pngthanh.cineverse.concession.dto;

import java.math.BigDecimal;

public record ConcessionAdminResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean active) {
}
