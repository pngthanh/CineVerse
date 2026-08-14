package com.pngthanh.cineverse.booking.service;

import com.pngthanh.cineverse.cinema.entity.Room;
import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class PricingService {
    private static final BigDecimal DEFAULT_WEEKDAY_PRICE = new BigDecimal("70000");
    private static final BigDecimal DEFAULT_WEEKEND_PRICE = new BigDecimal("100000");
    private static final BigDecimal DEFAULT_VIP_SURCHARGE = new BigDecimal("20000");
    private static final BigDecimal COUPLE_SURCHARGE = new BigDecimal("70000");

    public BigDecimal seatPrice(Showtime showtime, ShowtimeSeat seat) {
        BigDecimal surcharge = switch (seat.getSeat().getType()) {
            case NORMAL -> BigDecimal.ZERO;
            case VIP -> vipSurcharge(showtime.getRoom());
            case COUPLE -> COUPLE_SURCHARGE;
        };
        return showtime.getBasePrice().add(surcharge);
    }

    public BigDecimal basePrice(Room room, LocalDateTime startTime) {
        DayOfWeek day = startTime.getDayOfWeek();
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        if (weekend) {
            return room.getWeekendBasePrice() == null
                    ? DEFAULT_WEEKEND_PRICE
                    : room.getWeekendBasePrice();
        }
        return room.getWeekdayBasePrice() == null
                ? DEFAULT_WEEKDAY_PRICE
                : room.getWeekdayBasePrice();
    }

    private BigDecimal vipSurcharge(Room room) {
        return room.getVipSurcharge() == null
                ? DEFAULT_VIP_SURCHARGE
                : room.getVipSurcharge();
    }
}
