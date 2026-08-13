package com.pngthanh.cineverse.concession.dto;

import java.math.BigDecimal;

public record ConcessionItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price) {
}
