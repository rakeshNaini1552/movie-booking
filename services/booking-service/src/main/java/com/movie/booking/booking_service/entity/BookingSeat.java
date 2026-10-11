package com.movie.booking.booking_service.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "booking_seat")
public class BookingSeat {

    @EmbeddedId
    private BookingSeatId id;

    protected BookingSeat() {} // required by JPA

    public BookingSeat(BookingSeatId id) {
        this.id = id;
    }
}