package com.pngthanh.cineverse.showtime.repository;

import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findAllByShowtimeIdOrderBySeatRowIndexAscSeatColumnIndexAsc(Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowtimeSeat ss join fetch ss.seat where ss.showtime.id = :showtimeId and ss.seat.id in :seatIds order by ss.seat.id")
    List<ShowtimeSeat> findForUpdate(@Param("showtimeId") Long showtimeId, @Param("seatIds") List<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowtimeSeat ss join fetch ss.seat where ss.holdToken = :holdToken order by ss.seat.id")
    List<ShowtimeSeat> findByHoldTokenForUpdate(@Param("holdToken") String holdToken);

    @Modifying
    @Query("""
        update ShowtimeSeat ss set ss.status = :available, ss.heldByUserId = null,
        ss.holdToken = null, ss.holdExpiresAt = null
        where ss.status = :held and ss.holdExpiresAt < :now
        """)
    int releaseExpired(@Param("available") ShowtimeSeatStatus available,
                       @Param("held") ShowtimeSeatStatus held,
                       @Param("now") Instant now);
}
