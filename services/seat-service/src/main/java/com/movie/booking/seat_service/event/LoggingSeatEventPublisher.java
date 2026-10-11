package com.movie.booking.seat_service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Placeholder until events go to Kafka (Phase 5): only logs. */
@Slf4j
@Component
public class LoggingSeatEventPublisher implements SeatEventPublisher {

    @Override
    public void publish(SeatsReleased event) {
        log.info("SeatsReleased({}) show={} booking={} seats={}",
                event.reason(), event.showId(), event.bookingId(), event.seatIds());
    }
}
