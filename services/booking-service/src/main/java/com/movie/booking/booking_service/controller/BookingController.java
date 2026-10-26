package com.movie.booking.booking_service.controller;

import com.movie.booking.booking_service.dto.*;
import com.movie.booking.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateBookingResponse createBooking(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateBookingRequest createBookingRequest,
                                               @RequestHeader("idempotency-key") UUID idempotencyKey) {
        UUID userId = currentuserId(jwt);
        return new CreateBookingResponse(
                bookingService.createBooking(
                        new CreateBookingCommand(userId, createBookingRequest.showId(), createBookingRequest.seatIds(), idempotencyKey)
                ));
    }

    private static @NonNull UUID currentuserId(Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return userId;
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    }

    @PostMapping("/{bookingId}/payments")
    public PayBookingResponse pay(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID bookingId,
                                  @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                  @Valid @RequestBody PayBookingRequest request) {
        PayOutcome outcome = bookingService.pay(
                new PayBookingCommand(bookingId, request.paymentToken(), idempotencyKey, currentUserId(jwt)));
        return new PayBookingResponse(bookingId, outcome.bookingStatus(), outcome.paymentStatus());
    }

    @PostMapping("/{bookingId}/cancel")
    public CancelBookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        return new CancelBookingResponse(bookingId, bookingService.cancel(bookingId, currentUserId(jwt)));
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        PayableBooking booking = bookingService.getOwnedBooking(bookingId, currentUserId(jwt));
        return new BookingResponse(booking.bookingId(), booking.showId(), booking.status(),
                booking.totalAmount(), booking.seatIds());
    }

}
