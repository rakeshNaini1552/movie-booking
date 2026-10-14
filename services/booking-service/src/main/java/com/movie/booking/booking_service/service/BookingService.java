package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.client.SeatClient;

import com.movie.booking.booking_service.excepiton.SeatServiceUnavailableException;
import com.movie.booking.booking_service.excepiton.SeatsUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingStateService bookingStateService;
    private final SeatClient seatClient;

    public UUID createBooking(CreateBookingCommand command) {
        UUID bookingId = bookingStateService.createPending(command);

        try{
            seatClient.holdSeats(command.showId(), command.seatIds(), bookingId);
        }catch (SeatsUnavailableException | SeatServiceUnavailableException e) {
            log.warn("Seat hold failed for booking {}: {}", bookingId, e.getMessage());
            bookingStateService.markFailed(bookingId);
            throw e;
        }
        return bookingId;
    }
}
