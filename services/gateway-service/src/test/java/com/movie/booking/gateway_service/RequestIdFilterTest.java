package com.movie.booking.gateway_service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RequestIdFilterTest {

	// Stub for booking-service: records the X-Request-Id it receives and echoes it back like the real service does
	static final List<String> received = new CopyOnWriteArrayList<>();
	static HttpServer downstream;

	@Autowired
	ApplicationContext context;

	WebTestClient client;

	@BeforeAll
	static void startDownstream() throws IOException {
		downstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		downstream.createContext("/", exchange -> {
			String id = exchange.getRequestHeaders().getFirst(RequestId.HEADER);
			received.add(String.valueOf(id));
			if (id != null) {
				exchange.getResponseHeaders().set(RequestId.HEADER, id);
			}
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		downstream.start();
	}

	@AfterAll
	static void stopDownstream() {
		downstream.stop(0);
	}

	@DynamicPropertySource
	static void routeToStub(DynamicPropertyRegistry registry) throws IOException {
		// The stub must exist before the context reads the route URL
		if (downstream == null) {
			startDownstream();
		}
		registry.add("BOOKING_SERVICE_URL", () -> "http://localhost:" + downstream.getAddress().getPort());
	}

	@BeforeEach
	void setUp() {
		received.clear();
		client = WebTestClient.bindToApplicationContext(context)
				.apply(SecurityMockServerConfigurers.springSecurity())
				.build();
	}

	@Test
	void validIncomingIdIsForwardedAndReturnedUnchanged() {
		client.mutateWith(jwt()).get().uri("/api/v1/bookings")
				.header(RequestId.HEADER, "abc-123-DEF")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(RequestId.HEADER, "abc-123-DEF");

		assertThat(received).containsExactly("abc-123-DEF");
	}

	@Test
	void missingIdIsGeneratedForwardedAndReturned() {
		String returned = idReturnedFor(null);

		assertThat(UUID.fromString(returned)).isNotNull();
		assertThat(received).containsExactly(returned);
	}

	@Test
	void malformedIdsAreReplacedAndNotForwardedAsSent() {
		for (String bad : List.of("has space", "x".repeat(65), "semi;colon", "<script>")) {
			received.clear();
			String returned = idReturnedFor(bad);

			assertThat(returned).isNotEqualTo(bad);
			assertThat(RequestId.SAFE_ID.matcher(returned).matches()).isTrue();
			assertThat(received).containsExactly(returned);
		}
	}

	private String idReturnedFor(String incoming) {
		WebTestClient.RequestHeadersSpec<?> spec = client.mutateWith(jwt()).get().uri("/api/v1/bookings");
		if (incoming != null) {
			spec = spec.header(RequestId.HEADER, incoming);
		}
		List<String> values = spec.exchange().expectStatus().isOk()
				.returnResult(String.class).getResponseHeaders().get(RequestId.HEADER);
		assertThat(values).hasSize(1);
		return values.get(0);
	}

	// A fresh subject per request keeps the rate limiter out of the way whether or not Redis is running
	private static SecurityMockServerConfigurers.JwtMutator jwt() {
		return SecurityMockServerConfigurers.mockJwt().jwt(j -> j.subject(UUID.randomUUID().toString()));
	}

}
