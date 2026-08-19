package com.pngthanh.cineverse.voucher.repository;

import com.pngthanh.cineverse.voucher.entity.SavedVoucher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedVoucherRepository extends JpaRepository<SavedVoucher, Long> {
    List<SavedVoucher> findAllByUserIdOrderBySavedAtDesc(Long userId);
    Optional<SavedVoucher> findByUserIdAndVoucherId(Long userId, Long voucherId);
    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);
    void deleteByUserIdAndVoucherId(Long userId, Long voucherId);
    void deleteAllByVoucherId(Long voucherId);
}
