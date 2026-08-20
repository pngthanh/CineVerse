package com.pngthanh.cineverse.concession.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ConcessionAdminRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 220) String description,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
        boolean active) {
}
