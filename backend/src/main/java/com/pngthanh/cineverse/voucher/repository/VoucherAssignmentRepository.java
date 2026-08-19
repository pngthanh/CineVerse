package com.pngthanh.cineverse.voucher.repository;

import com.pngthanh.cineverse.voucher.entity.VoucherAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherAssignmentRepository extends JpaRepository<VoucherAssignment, Long> {
    boolean existsByVoucherIdAndUserId(Long voucherId, Long userId);
    List<VoucherAssignment> findAllByVoucherId(Long voucherId);
    void deleteAllByVoucherId(Long voucherId);
}
