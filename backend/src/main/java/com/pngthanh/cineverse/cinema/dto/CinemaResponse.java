package com.pngthanh.cineverse.cinema.dto;

import java.util.List;

public record CinemaResponse(
        Long id,
        String name,
        String address,
        boolean active,
        List<RoomResponse> rooms) {

    public record RoomResponse(
            Long id,
            String name,
            boolean active,
            int seatCount) {
    }
}
