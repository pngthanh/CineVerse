package com.pngthanh.cineverse.cinema.entity;

import com.pngthanh.cineverse.common.enums.SeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(name = "uk_room_seat_code", columnNames = {"room_id", "seat_code"}))
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id")
    private Room room;
    @Column(name = "seat_code", nullable = false, length = 20)
    private String seatCode;
    @Column(nullable = false)
    private Integer rowIndex;
    @Column(nullable = false)
    private Integer columnIndex;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SeatType type = SeatType.NORMAL;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }
    public Integer getRowIndex() { return rowIndex; }
    public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
    public Integer getColumnIndex() { return columnIndex; }
    public void setColumnIndex(Integer columnIndex) { this.columnIndex = columnIndex; }
    public SeatType getType() { return type; }
    public void setType(SeatType type) { this.type = type; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
