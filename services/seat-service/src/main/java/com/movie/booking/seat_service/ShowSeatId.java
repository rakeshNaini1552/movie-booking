package com.movie.booking.seat_service;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@ToString
public class ShowSeatId implements Serializable {

    @Column(name = "show_id")  private UUID showId;
    @Column(name = "seat_id")  private String seatId;

    protected ShowSeatId() {} // required by JPA

    public ShowSeatId(UUID showId, String seatId) {
        this.showId = showId;
        this.seatId = seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ShowSeatId that = (ShowSeatId) o;
        return Objects.equals(showId, that.showId) && Objects.equals(seatId, that.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showId, seatId);
    }
}
