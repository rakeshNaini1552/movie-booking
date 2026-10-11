package com.movie.booking.seat_service.event;

/** Where seat events go. Business code depends on this, not on the transport behind it. */
public interface SeatEventPublisher {

    void publish(SeatsReleased event);
}
