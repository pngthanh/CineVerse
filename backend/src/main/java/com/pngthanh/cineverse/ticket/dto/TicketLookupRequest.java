package com.pngthanh.cineverse.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketLookupRequest(@NotBlank String ticketCode) {
}
