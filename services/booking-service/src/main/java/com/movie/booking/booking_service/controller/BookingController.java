package com.movie.booking.booking_service.controller;

import com.movie.booking.booking_service.CreateBookingCommand;
import com.movie.booking.booking_service.dto.CreateBookingRequest;
import com.movie.booking.booking_service.dto.CreateBookingResponse;
import com.movie.booking.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateBookingResponse createBooking(@Valid @RequestBody CreateBookingRequest createBookingRequest){
        return new CreateBookingResponse(
                bookingService.createBooking(
                    new CreateBookingCommand(createBookingRequest.userId(), createBookingRequest.showId(), createBookingRequest.seatIds())
            ));
    }

}
