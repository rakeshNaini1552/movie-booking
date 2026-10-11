package com.movie.booking.booking_service.entity;

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
public class BookingSeatId implements Serializable {

    @Column(name = "booking_id") private UUID bookingId;
    @Column(name = "seat_id")    private String seatId;

    protected BookingSeatId() {} // required by JPA

    public BookingSeatId(UUID bookingId, String seatId) {
        this.bookingId = bookingId;
        this.seatId = seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BookingSeatId that = (BookingSeatId) o;
        return Objects.equals(bookingId, that.bookingId) && Objects.equals(seatId, that.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, seatId);
    }
}