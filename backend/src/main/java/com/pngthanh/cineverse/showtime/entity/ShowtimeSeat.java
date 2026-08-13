package com.pngthanh.cineverse.showtime.entity;

import com.pngthanh.cineverse.cinema.entity.Seat;
import com.pngthanh.cineverse.common.enums.ShowtimeSeatStatus;
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
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "showtime_seats", uniqueConstraints = @UniqueConstraint(name = "uk_showtime_seat", columnNames = {"showtime_id", "seat_id"}))
public class ShowtimeSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "showtime_id")
    private Showtime showtime;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seat_id")
    private Seat seat;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ShowtimeSeatStatus status = ShowtimeSeatStatus.AVAILABLE;
    private Long heldByUserId;
    private String holdToken;
    private Instant holdExpiresAt;
    @Version
    private Long version;

    public Long getId() { return id; }
    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }
    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }
    public ShowtimeSeatStatus getStatus() { return status; }
    public void setStatus(ShowtimeSeatStatus status) { this.status = status; }
    public Long getHeldByUserId() { return heldByUserId; }
    public void setHeldByUserId(Long heldByUserId) { this.heldByUserId = heldByUserId; }
    public String getHoldToken() { return holdToken; }
    public void setHoldToken(String holdToken) { this.holdToken = holdToken; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(Instant holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
}
