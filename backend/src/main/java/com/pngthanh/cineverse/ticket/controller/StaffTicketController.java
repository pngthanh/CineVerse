package com.pngthanh.cineverse.ticket.controller;

import com.pngthanh.cineverse.ticket.dto.StaffTicketResponse;
import com.pngthanh.cineverse.ticket.dto.TicketLookupRequest;
import com.pngthanh.cineverse.ticket.service.TicketService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/staff/tickets")
public class StaffTicketController {
    private final TicketService ticketService;

    public StaffTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/lookup")
    public StaffTicketResponse lookup(
            Principal principal,
            @Valid @RequestBody TicketLookupRequest request) {
        return ticketService.lookupManual(principal.getName(), request.ticketCode());
    }

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StaffTicketResponse scan(
            Principal principal,
            @RequestPart("file") MultipartFile file) {
        return ticketService.lookupQr(principal.getName(), file);
    }

    @PostMapping("/check-in")
    public StaffTicketResponse checkIn(
            Principal principal,
            @Valid @RequestBody TicketLookupRequest request) {
        return ticketService.checkIn(principal.getName(), request.ticketCode());
    }
}
