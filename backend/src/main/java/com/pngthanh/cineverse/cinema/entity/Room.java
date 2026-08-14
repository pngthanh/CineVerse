package com.pngthanh.cineverse.cinema.entity;

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
@Table(
        name = "rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_name_cinema",
                columnNames = {"cinema_id", "name"}))
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "row_count")
    private Integer rowCount = 8;

    @Column(name = "seats_per_row")
    private Integer seatsPerRow = 10;

    @Column(name = "weekday_base_price", precision = 12, scale = 0)
    private BigDecimal weekdayBasePrice = new BigDecimal("70000");

    @Column(name = "weekend_base_price", precision = 12, scale = 0)
    private BigDecimal weekendBasePrice = new BigDecimal("100000");

    @Column(name = "vip_surcharge", precision = 12, scale = 0)
    private BigDecimal vipSurcharge = new BigDecimal("20000");

    public Long getId() {
        return id;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getSeatsPerRow() {
        return seatsPerRow;
    }

    public void setSeatsPerRow(Integer seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public BigDecimal getWeekdayBasePrice() {
        return weekdayBasePrice;
    }

    public void setWeekdayBasePrice(BigDecimal weekdayBasePrice) {
        this.weekdayBasePrice = weekdayBasePrice;
    }

    public BigDecimal getWeekendBasePrice() {
        return weekendBasePrice;
    }

    public void setWeekendBasePrice(BigDecimal weekendBasePrice) {
        this.weekendBasePrice = weekendBasePrice;
    }

    public BigDecimal getVipSurcharge() {
        return vipSurcharge;
    }

    public void setVipSurcharge(BigDecimal vipSurcharge) {
        this.vipSurcharge = vipSurcharge;
    }
}
