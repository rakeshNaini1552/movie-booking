package com.movie.booking.ticketservice.controller;

import com.movie.booking.ticketservice.dto.ApiError;
import com.movie.booking.ticketservice.dto.ScanRequest;
import com.movie.booking.ticketservice.dto.ScanResponse;
import com.movie.booking.ticketservice.dto.TicketResponse;
import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Tickets", description = "Ticket lookup and QR scanning")
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "Get the ticket of a booking",
            description = "Returns 404 until the BookingConfirmed event has been consumed and the ticket issued.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "No ticket issued for this booking yet", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/by-booking/{bookingId}")
    public TicketResponse getByBooking(@PathVariable UUID bookingId) {
        return TicketResponse.from(ticketService.getByBookingId(bookingId));
    }

    @Operation(summary = "Get a ticket by id")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(@PathVariable UUID ticketId) {
        return TicketResponse.from(ticketService.getTicket(ticketId));
    }

    @Operation(summary = "Scan a ticket QR code",
            description = "Requires the staff role. The first valid scan marks the ticket USED; later scans are rejected.")
    @ApiResponse(responseCode = "400", description = "Invalid QR payload, payload belongs to another ticket, or request body invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "403", description = "Token lacks the staff role")
    @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Ticket is already USED or VOID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{ticketId}/scan")
    public ScanResponse scan(@PathVariable UUID ticketId, @Valid @RequestBody ScanRequest request) {
        Ticket ticket = ticketService.scan(ticketId, request.payload());
        return new ScanResponse(ticket.getTicketId(), ticket.getStatus(), ticket.getUsedAt());
    }
}
