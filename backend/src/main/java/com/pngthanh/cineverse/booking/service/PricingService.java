package com.pngthanh.cineverse.booking.service;

import com.pngthanh.cineverse.showtime.entity.Showtime;
import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PricingService {
    private static final BigDecimal VIP_SURCHARGE = new BigDecimal("20000");
    private static final BigDecimal COUPLE_SURCHARGE = new BigDecimal("70000");

    public BigDecimal seatPrice(Showtime showtime, ShowtimeSeat seat) {
        BigDecimal surcharge = switch (seat.getSeat().getType()) {
            case NORMAL -> BigDecimal.ZERO;
            case VIP -> VIP_SURCHARGE;
            case COUPLE -> COUPLE_SURCHARGE;
        };
        return showtime.getBasePrice().add(surcharge);
    }
}
