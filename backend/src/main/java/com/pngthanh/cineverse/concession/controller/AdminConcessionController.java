package com.pngthanh.cineverse.concession.controller;

import com.pngthanh.cineverse.concession.dto.ConcessionAdminRequest;
import com.pngthanh.cineverse.concession.dto.ConcessionAdminResponse;
import com.pngthanh.cineverse.concession.service.ConcessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/concessions")
public class AdminConcessionController {
    private final ConcessionService concessionService;

    public AdminConcessionController(ConcessionService concessionService) {
        this.concessionService = concessionService;
    }

    @GetMapping
    public List<ConcessionAdminResponse> list() {
        return concessionService.listAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConcessionAdminResponse create(@Valid @RequestBody ConcessionAdminRequest request) {
        return concessionService.create(request);
    }

    @PutMapping("/{id}")
    public ConcessionAdminResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ConcessionAdminRequest request) {
        return concessionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        concessionService.deactivate(id);
    }
}
