package com.pngthanh.cineverse.concession.repository;

import com.pngthanh.cineverse.concession.entity.ConcessionItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcessionItemRepository extends JpaRepository<ConcessionItem, Long> {
    List<ConcessionItem> findAllByActiveTrueOrderByIdAsc();

    Optional<ConcessionItem> findByNameIgnoreCase(String name);
}
