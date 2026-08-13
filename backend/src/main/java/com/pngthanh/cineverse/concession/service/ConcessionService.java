package com.pngthanh.cineverse.concession.service;

import com.pngthanh.cineverse.booking.dto.CreateBookingRequest;
import com.pngthanh.cineverse.common.exception.ApiException;
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
