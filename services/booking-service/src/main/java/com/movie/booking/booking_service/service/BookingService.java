package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.CreateBookingCommand;
import com.movie.booking.booking_service.client.SeatClient;

import com.movie.booking.booking_service.excepiton.SeatsUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingStateService bookingStateService;
    private final SeatClient seatClient;

    public UUID createBooking(CreateBookingCommand command) {
        UUID bookingId = bookingStateService.createPending(command);

        try{
            seatClient.holdSeats(command.showId(), command.seatIds(), bookingId);
        }catch(SeatsUnavailableException e)
        {
            bookingStateService.markFailed(bookingId);
            System.out.println("Seats not available to hold" + e.getMessage());
            throw e;
        }
        return bookingId;
    }
}
