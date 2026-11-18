package com.movie.booking.booking_service.controller;

import com.movie.booking.booking_service.dto.*;
import com.movie.booking.booking_service.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import com.movie.booking.booking_service.exception.InvalidPageRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@Tag(name = "Bookings", description = "Booking, payment and cancellation for the signed-in user")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

    private final BookingService bookingService;

    @Operation(summary = "Create a booking and hold its seats",
            description = "The user id comes from the JWT subject. Repeating a request with the same idempotency-key returns the original booking id.")
    @ApiResponse(responseCode = "400", description = "Validation failed, malformed body, or missing/malformed idempotency-key")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "409", description = "Seats are unavailable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "503", description = "Seat service is unavailable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
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

    @Operation(summary = "Pay for a booking",
            description = "The user id comes from the JWT subject. A declined payment returns 200 with status PENDING and paymentStatus FAILED. If payment succeeds but the booking cannot be completed, it returns 200 with status FAILED and a refund is requested.")
    @ApiResponse(responseCode = "400", description = "Invalid payment token, validation failure, or missing/malformed Idempotency-Key", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Booking not found or not owned by the user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Booking is not payable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "503", description = "Payment service is unavailable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{bookingId}/payments")
    public PayBookingResponse pay(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID bookingId,
                                  @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                  @Valid @RequestBody PayBookingRequest request) {
        PayOutcome outcome = bookingService.pay(
                new PayBookingCommand(bookingId, request.paymentToken(), idempotencyKey, currentUserId(jwt)));
        return new PayBookingResponse(bookingId, outcome.bookingStatus(), outcome.paymentStatus());
    }

    @Operation(summary = "Cancel a booking",
            description = "The user id comes from the JWT subject. Cancelling an already cancelled booking returns CANCELLED.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Booking not found or not owned by the user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Booking is FAILED or EXPIRED, or its ticket was already used", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{bookingId}/cancel")
    public CancelBookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        return new CancelBookingResponse(bookingId, bookingService.cancel(bookingId, currentUserId(jwt)));
    }

    @Operation(summary = "List the signed-in user's bookings",
            description = "The user id comes from the JWT subject. Newest first. page is zero-based; size defaults to 20 and is capped at 50.")
    @ApiResponse(responseCode = "400", description = "page is negative or size is below 1, or either is not a number", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @GetMapping
    public BookingPage listBookings(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("page must be 0 or greater");
        }
        if (size < 1) {
            throw new InvalidPageRequestException("size must be 1 or greater");
        }
        return bookingService.listBookings(currentUserId(jwt), page, Math.min(size, MAX_PAGE_SIZE));
    }

    @Operation(summary = "Get a booking",
            description = "The user id comes from the JWT subject. A booking owned by another user is reported as not found.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Booking not found or not owned by the user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        PayableBooking booking = bookingService.getOwnedBooking(bookingId, currentUserId(jwt));
        return new BookingResponse(booking.bookingId(), booking.showId(), booking.status(),
                booking.totalAmount(), booking.seatIds());
    }

}
