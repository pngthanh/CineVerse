package com.pngthanh.cineverse.cinema.repository;

import com.pngthanh.cineverse.cinema.entity.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByRoomIdOrderByRowIndexAscColumnIndexAsc(Long roomId);

    void deleteAllByRoomId(Long roomId);
}
