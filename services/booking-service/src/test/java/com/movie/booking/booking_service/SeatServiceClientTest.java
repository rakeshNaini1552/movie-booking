package com.movie.booking.booking_service;

import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import com.movie.booking.booking_service.exception.SeatsUnavailableException;
import com.movie.booking.booking_service.service.SeatServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SeatServiceClientTest {

    private static final String BASE_URL = "http://seat-service";

    private MockRestServiceServer server;
    private SeatServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SeatServiceClient(builder.build());
    }

    @Test
    void sends_hold_request_to_seat_service_and_returns_on_201() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/v1/shows/" + showId + "/holds"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.seatIds[0]").value("A1"))
                .andExpect(jsonPath("$.seatIds[1]").value("A2"))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.holdSeats(showId, List.of("A1", "A2"), bookingId);

        server.verify();
    }

    @Test
    void conflict_from_seat_service_becomes_SeatsUnavailableException() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/v1/shows/" + showId + "/holds"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.holdSeats(showId, List.of("A1"), bookingId))
                .isInstanceOf(SeatsUnavailableException.class);
    }

    @Test
    void unreachable_seat_service_becomes_SeatServiceUnavailableException() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/v1/shows/" + showId + "/holds"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.holdSeats(showId, List.of("A1"), bookingId))
                .isInstanceOf(SeatServiceUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void server_error_from_seat_service_becomes_SeatServiceUnavailableException() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/v1/shows/" + showId + "/holds"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.holdSeats(showId, List.of("A1"), bookingId))
                .isInstanceOf(SeatServiceUnavailableException.class)
                .hasCauseInstanceOf(HttpServerErrorException.class);
    }
}
