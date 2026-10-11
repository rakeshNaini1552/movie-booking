package com.movie.booking.seat_service;

import com.movie.booking.seat_service.event.SeatEventPublisher;
import com.movie.booking.seat_service.event.SeatsReleased;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test publisher: remembers every event so a test can look at them. */
public class RecordingSeatEventPublisher implements SeatEventPublisher {

    private final List<SeatsReleased> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(SeatsReleased event) {
        events.add(event);
    }

    public List<SeatsReleased> eventsFor(UUID showId) {
        return events.stream().filter(e -> e.showId().equals(showId)).toList();
    }
}
