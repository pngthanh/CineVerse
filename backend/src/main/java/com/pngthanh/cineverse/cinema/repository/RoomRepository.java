package com.pngthanh.cineverse.cinema.repository;
import com.pngthanh.cineverse.cinema.entity.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoomRepository extends JpaRepository<Room, Long> { List<Room> findAllByCinemaIdOrderByName(Long cinemaId); }
