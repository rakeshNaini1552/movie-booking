package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.SeatServiceClient;
import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class SeatServiceClientReleaseTest {

    private static final String BASE_URL = "http://seat-service";

    private MockRestServiceServer server;
    private SeatServiceClient client;
    private UUID showId;
    private UUID bookingId;
    private String url;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SeatServiceClient(builder.build());
        showId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        url = BASE_URL + "/api/v1/shows/" + showId + "/holds/" + bookingId + "/release";
    }

    @Test
    void posts_seat_ids_to_the_release_url_and_accepts_204() {
        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.seatIds[0]").value("A1"))
                .andExpect(jsonPath("$.seatIds[1]").value("A2"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatCode(() -> client.releaseSeats(showId, List.of("A1", "A2"), bookingId))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void server_error_becomes_SeatServiceUnavailableException() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.releaseSeats(showId, List.of("A1"), bookingId))
                .isInstanceOf(SeatServiceUnavailableException.class)
                .hasCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void connection_failure_becomes_SeatServiceUnavailableException() {
        server.expect(requestTo(url))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.releaseSeats(showId, List.of("A1"), bookingId))
                .isInstanceOf(SeatServiceUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }
}