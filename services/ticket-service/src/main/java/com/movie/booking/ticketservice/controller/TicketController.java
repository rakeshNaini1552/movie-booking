package com.movie.booking.ticketservice.controller;

import com.movie.booking.ticketservice.dto.ScanRequest;
import com.movie.booking.ticketservice.dto.ScanResponse;
import com.movie.booking.ticketservice.dto.TicketResponse;
import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/by-booking/{bookingId}")
    public TicketResponse getByBooking(@PathVariable UUID bookingId) {
        return TicketResponse.from(ticketService.getByBookingId(bookingId));
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(@PathVariable UUID ticketId) {
        return TicketResponse.from(ticketService.getTicket(ticketId));
    }

    @PostMapping("/{ticketId}/scan")
    public ScanResponse scan(@PathVariable UUID ticketId, @Valid @RequestBody ScanRequest request) {
        Ticket ticket = ticketService.scan(ticketId, request.payload());
        return new ScanResponse(ticket.getTicketId(), ticket.getStatus(), ticket.getUsedAt());
    }
}
