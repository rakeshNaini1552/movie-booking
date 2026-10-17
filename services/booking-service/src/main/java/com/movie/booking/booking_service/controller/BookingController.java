package com.movie.booking.booking_service.controller;

import com.movie.booking.booking_service.dto.*;
import com.movie.booking.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateBookingResponse createBooking(@Valid @RequestBody CreateBookingRequest createBookingRequest, @RequestHeader("idempotency-key")UUID idempotencyKey){
        return new CreateBookingResponse(
                bookingService.createBooking(
                    new CreateBookingCommand(createBookingRequest.userId(), createBookingRequest.showId(), createBookingRequest.seatIds(), idempotencyKey)
            ));
    }

    @PostMapping("/{bookingId}/payments")
    public PayBookingResponse pay(@PathVariable UUID bookingId,
                                  @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                  @Valid @RequestBody PayBookingRequest request) {
        PayOutcome outcome = bookingService.pay(
                new PayBookingCommand(bookingId, request.paymentToken(), idempotencyKey));
        return new PayBookingResponse(bookingId, outcome.bookingStatus(), outcome.paymentStatus());
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@PathVariable UUID bookingId) {
        PayableBooking booking = bookingService.getBooking(bookingId);
        return new BookingResponse(booking.bookingId(), booking.showId(), booking.status(),
                booking.totalAmount(), booking.seatIds());
    }

}
