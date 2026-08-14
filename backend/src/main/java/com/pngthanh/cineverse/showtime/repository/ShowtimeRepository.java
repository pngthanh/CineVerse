package com.pngthanh.cineverse.showtime.repository;

import com.pngthanh.cineverse.showtime.entity.Showtime;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findAllByActiveTrueOrderByStartTime();

    List<Showtime> findAllByMovieIdAndActiveTrueOrderByStartTime(Long movieId);

    List<Showtime> findAllByRoomCinemaIdAndActiveTrueOrderByStartTime(Long cinemaId);

    List<Showtime> findAllByRoomIdAndActiveTrueOrderByStartTime(Long roomId);

    List<Showtime> findAllByMovieIdAndRoomCinemaIdAndActiveTrueOrderByStartTime(
            Long movieId,
            Long cinemaId);

    List<Showtime> findAllByMovieIdAndStartTimeBetweenAndActiveTrueOrderByStartTime(
            Long movieId,
            LocalDateTime from,
            LocalDateTime to);

    List<Showtime> findAllByRoomCinemaIdAndStartTimeBetweenAndActiveTrueOrderByStartTime(
            Long cinemaId,
            LocalDateTime from,
            LocalDateTime to);

    boolean existsByRoomId(Long roomId);

    boolean existsByRoomIdAndActiveTrueAndStartTimeAfter(Long roomId, LocalDateTime time);

    boolean existsByRoomCinemaIdAndActiveTrueAndStartTimeAfter(Long cinemaId, LocalDateTime time);

    @Query("""
        select count(s) > 0 from Showtime s
        where s.room.id = :roomId and s.active = true
          and (:excludeId is null or s.id <> :excludeId)
          and s.startTime < :endTime and s.endTime > :startTime
        """)
    boolean hasConflict(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId);
}
