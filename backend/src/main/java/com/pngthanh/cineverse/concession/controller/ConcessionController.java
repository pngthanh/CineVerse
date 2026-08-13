package com.pngthanh.cineverse.concession.controller;

import com.pngthanh.cineverse.concession.dto.ConcessionItemResponse;
import com.pngthanh.cineverse.concession.service.ConcessionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concessions")
public class ConcessionController {
    private final ConcessionService concessionService;

    public ConcessionController(ConcessionService concessionService) {
        this.concessionService = concessionService;
    }

    @GetMapping
    public List<ConcessionItemResponse> list() {
        return concessionService.listActive();
    }
}
