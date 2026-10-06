package com.movie.booking.seat_service.entity;

import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.ShowSeatId;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "show_seat")
public class ShowSeat {
    @EmbeddedId private ShowSeatId id;

    @Column(name = "seat_row")    private String row;
    @Column(name = "seat_number") private int number;

    @Enumerated(EnumType.STRING)     // hint: STRING, not ORDINAL. Why?
    @Column(name = "seat_type")   private SeatType type;

    @Enumerated(EnumType.STRING)     private SeatStatus status;
    @Column(name = "booking_id")  private UUID bookingId;

    @Version private long version;    // hint: JPA optimistic-locking annotation
    @Column(name = "updated_at")  private Instant updatedAt;

    protected ShowSeat() {}
    // getters only for now


}
