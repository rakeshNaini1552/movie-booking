package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.SeatHoldRequest;
import com.movie.booking.booking_service.excepiton.SeatsUnavailableException;
import com.movie.booking.booking_service.excepiton.SeatServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SeatServiceClient implements SeatClient {
    private final RestClient restClient;


    @Override
    public void holdSeats(UUID showId, List<String> seatIds, UUID bookingId) {
        try {
            restClient.post()
                    .uri("/api/v1/shows/{showId}/holds", showId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SeatHoldRequest(seatIds, bookingId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new SeatsUnavailableException("Seats " + seatIds + " unavailable for booking " + bookingId);
        }catch (ResourceAccessException e) {                     // NEW: seat-service didn't answer
            throw new SeatServiceUnavailableException("Seat service unreachable for booking " + bookingId, e);
        }
    }
}
