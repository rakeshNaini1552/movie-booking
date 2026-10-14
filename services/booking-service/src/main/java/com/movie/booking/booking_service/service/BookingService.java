package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.client.SeatClient;

import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import com.movie.booking.booking_service.exception.SeatsUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingStateService bookingStateService;
    private final SeatClient seatClient;

    public UUID createBooking(CreateBookingCommand command) {
        Optional<UUID> existing = bookingStateService.findBookingIdByKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        UUID bookingId;
        try {
            bookingId = bookingStateService.createPending(command);
        } catch (DataIntegrityViolationException e) {
            return bookingStateService.findBookingIdByKey(command.idempotencyKey())
                    .orElseThrow(() -> e);
        }

        try {
            seatClient.holdSeats(command.showId(), command.seatIds(), bookingId);
        } catch (SeatsUnavailableException | SeatServiceUnavailableException e) {
            log.warn("Seat hold failed for booking {}: {}", bookingId, e.getMessage());
            bookingStateService.markFailed(bookingId);
            throw e;
        }
        return bookingId;
    }
}
