package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.SeatHoldRequest;
import com.movie.booking.booking_service.exception.SeatsUnavailableException;
import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SeatServiceClient implements SeatClient {
    private final RestClient restClient;


    @Retry(name = "seatService")
    @CircuitBreaker(name = "seatService", fallbackMethod = "holdSeatsFallback")
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
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new SeatServiceUnavailableException("Seat service failed for booking " + bookingId, e);
        }
    }

    private void holdSeatsFallback(UUID showId, List<String> seatIds, UUID bookingId,
                                   CallNotPermittedException e) {
        throw new SeatServiceUnavailableException(
                "Seat service circuit is open, rejecting hold for booking " + bookingId, e);
    }
}
