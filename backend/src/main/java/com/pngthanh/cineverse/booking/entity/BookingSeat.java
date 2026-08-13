package com.pngthanh.cineverse.booking.entity;

import com.pngthanh.cineverse.showtime.entity.ShowtimeSeat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_seats", uniqueConstraints = @UniqueConstraint(name = "uk_booking_showtime_seat", columnNames = {"booking_id", "showtime_seat_id"}))
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id")
    private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "showtime_seat_id")
    private ShowtimeSeat showtimeSeat;
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public ShowtimeSeat getShowtimeSeat() { return showtimeSeat; }
    public void setShowtimeSeat(ShowtimeSeat showtimeSeat) { this.showtimeSeat = showtimeSeat; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
