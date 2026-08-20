package com.pngthanh.cineverse.concession.service;

import com.pngthanh.cineverse.booking.dto.CreateBookingRequest;
import com.pngthanh.cineverse.common.exception.ApiException;
import com.pngthanh.cineverse.concession.dto.ConcessionAdminRequest;
import com.pngthanh.cineverse.concession.dto.ConcessionAdminResponse;
import com.pngthanh.cineverse.concession.dto.ConcessionItemResponse;
import com.pngthanh.cineverse.concession.entity.ConcessionItem;
import com.pngthanh.cineverse.concession.repository.ConcessionItemRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcessionService {
    private final ConcessionItemRepository items;

    public ConcessionService(ConcessionItemRepository items) {
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<ConcessionAdminResponse> listAdmin() {
        return items.findAllByOrderByIdAsc().stream().map(this::toAdminResponse).toList();
    }

    @Transactional
    public ConcessionAdminResponse create(ConcessionAdminRequest request) {
        String name = normalizeName(request.name());
        if (items.findByNameIgnoreCase(name).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CONCESSION_NAME_EXISTS",
                    "Tên món bắp nước đã tồn tại.");
        }
        ConcessionItem item = new ConcessionItem();
        apply(item, request, name);
        return toAdminResponse(items.save(item));
    }

    @Transactional
    public ConcessionAdminResponse update(Long id, ConcessionAdminRequest request) {
        ConcessionItem item = requireItem(id);
        String name = normalizeName(request.name());
        items.findByNameIgnoreCase(name).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CONCESSION_NAME_EXISTS",
                    "Tên món bắp nước đã tồn tại.");
        });
        apply(item, request, name);
        return toAdminResponse(item);
    }

    @Transactional
    public void deactivate(Long id) {
        requireItem(id).setActive(false);
    }

    @Transactional(readOnly = true)
    public List<ConcessionItemResponse> listActive() {
        return items.findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(item -> new ConcessionItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getDescription(),
                        item.getPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConcessionQuote quote(List<CreateBookingRequest.ConcessionSelection> selections) {
        if (selections == null || selections.isEmpty()) {
            return new ConcessionQuote(List.of(), BigDecimal.ZERO);
        }

        List<SelectedConcession> selected = new ArrayList<>();
        Set<Long> uniqueIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateBookingRequest.ConcessionSelection selection : selections) {
            if (!uniqueIds.add(selection.itemId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "CONCESSION_DUPLICATE",
                        "Mỗi món chỉ được gửi một lần trong booking.");
            }

            ConcessionItem item = items.findById(selection.itemId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "CONCESSION_NOT_FOUND",
                            "Không tìm thấy món bắp nước đã chọn."));
            if (!item.isActive()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "CONCESSION_INACTIVE",
                        "Món bắp nước này hiện không khả dụng.");
            }

            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(selection.quantity()));
            selected.add(new SelectedConcession(item, selection.quantity(), lineTotal));
            total = total.add(lineTotal);
        }

        return new ConcessionQuote(List.copyOf(selected), total);
    }

    private ConcessionItem requireItem(Long id) {
        return items.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "CONCESSION_NOT_FOUND",
                "Không tìm thấy món bắp nước."));
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private void apply(ConcessionItem item, ConcessionAdminRequest request, String name) {
        item.setName(name);
        String description = request.description() == null ? null : request.description().trim();
        item.setDescription(description == null || description.isBlank() ? null : description);
        item.setPrice(request.price());
        item.setActive(request.active());
    }

    private ConcessionAdminResponse toAdminResponse(ConcessionItem item) {
        return new ConcessionAdminResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isActive());
    }

    public record SelectedConcession(
            ConcessionItem item,
            int quantity,
            BigDecimal totalAmount) {
    }

    public record ConcessionQuote(
            List<SelectedConcession> selections,
            BigDecimal totalAmount) {
    }
}
