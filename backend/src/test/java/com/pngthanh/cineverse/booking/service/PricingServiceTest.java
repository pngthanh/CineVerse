package com.pngthanh.cineverse.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.common.enums.SeatType;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingServiceTest {
    private final PricingService service = new PricingService();

    @Test
    void normalSeatUsesBasePrice() {
        assertEquals(new BigDecimal("70000"), priceFor(SeatType.NORMAL));
    }

    @Test
    void vipSeatAddsTwentyThousandVnd() {
        assertEquals(new BigDecimal("90000"), priceFor(SeatType.VIP));
    }

    @Test
    void coupleSeatAddsSeventyThousandVnd() {
        assertEquals(new BigDecimal("140000"), priceFor(SeatType.COUPLE));
    }

    private BigDecimal priceFor(SeatType seatType) {
        Showtime showtime = new Showtime();
        showtime.setBasePrice(new BigDecimal("70000"));

        Seat seat = new Seat();
        seat.setType(seatType);

        ShowtimeSeat showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setSeat(seat);
        return service.seatPrice(showtime, showtimeSeat);
    }
}
