package com.pngthanh.cineverse.ticket.controller;

import com.pngthanh.cineverse.ticket.service.TicketService;
import java.security.Principal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping(value = "/{ticketCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(Principal principal, @PathVariable String ticketCode) {
        return ticketService.qrPng(principal.getName(), ticketCode);
    }

    @GetMapping(value = "/{ticketCode}/download", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> download(Principal principal, @PathVariable String ticketCode) {
        byte[] content = ticketService.downloadableTicket(principal.getName(), ticketCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("CineVerse-" + ticketCode + ".png")
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
