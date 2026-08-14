package com.pngthanh.cineverse.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.common.enums.SeatType;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PricingServiceTest {
    private final PricingService service = new PricingService();

    @Test
    void normalSeatUsesBasePrice() {
        assertEquals(new BigDecimal("70000"), priceFor(SeatType.NORMAL, new BigDecimal("20000")));
    }

    @Test
    void vipSeatUsesRoomConfiguredSurcharge() {
        assertEquals(new BigDecimal("95000"), priceFor(SeatType.VIP, new BigDecimal("25000")));
    }

    @Test
    void coupleSeatAddsSeventyThousandVnd() {
        assertEquals(new BigDecimal("140000"), priceFor(SeatType.COUPLE, new BigDecimal("20000")));
    }

    @Test
    void weekendUsesRoomWeekendBasePrice() {
        Room room = new Room();
        room.setWeekdayBasePrice(new BigDecimal("70000"));
        room.setWeekendBasePrice(new BigDecimal("100000"));

        assertEquals(
                new BigDecimal("100000"),
                service.basePrice(room, LocalDateTime.of(2026, 8, 16, 19, 0)));
    }

    private BigDecimal priceFor(SeatType seatType, BigDecimal vipSurcharge) {
        Room room = new Room();
        room.setVipSurcharge(vipSurcharge);

        Showtime showtime = new Showtime();
        showtime.setRoom(room);
        showtime.setBasePrice(new BigDecimal("70000"));

        Seat seat = new Seat();
        seat.setRoom(room);
        seat.setType(seatType);

        ShowtimeSeat showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setSeat(seat);
        return service.seatPrice(showtime, showtimeSeat);
    }
}
