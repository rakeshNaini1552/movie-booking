package com.movie.booking.gateway_service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.StatusAssertions;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// Stub servers stand in for booking-, ticket- and catalog-service; each answers "<name> <path and query it received>",
// so the body shows both which service the gateway forwarded to and the path it forwarded.
@SpringBootTest
@Import(TestContainersConfig.class)
class TicketRouteIntegrationTest {

	private static final AtomicInteger TICKET_HITS = new AtomicInteger();
	private static final HttpServer BOOKING = stub("booking-service", new AtomicInteger());
	private static final HttpServer TICKET = stub("ticket-service", TICKET_HITS);
	private static final HttpServer CATALOG = stub("catalog-service", new AtomicInteger());

	@DynamicPropertySource
	static void downstreams(DynamicPropertyRegistry registry) {
		registry.add("BOOKING_SERVICE_URL", () -> "http://localhost:" + BOOKING.getAddress().getPort());
		registry.add("TICKET_SERVICE_URL", () -> "http://localhost:" + TICKET.getAddress().getPort());
		registry.add("CATALOG_SERVICE_URL", () -> "http://localhost:" + CATALOG.getAddress().getPort());
	}

	@AfterAll
	static void stopStubs() {
		BOOKING.stop(0);
		TICKET.stop(0);
		CATALOG.stop(0);
	}

	@Autowired
	ApplicationContext context;

	WebTestClient client;

	@BeforeEach
	void setUp() {
		TICKET_HITS.set(0);
		client = WebTestClient.bindToApplicationContext(context)
				.apply(SecurityMockServerConfigurers.springSecurity())
				.build();
	}

	@Test
	void ticketByIdGoesToTicketServiceWithTheRewrittenPath() {
		UUID ticketId = UUID.randomUUID();

		assertThat(bodyOf(HttpMethod.GET, "/api/v1/tickets/" + ticketId))
				.isEqualTo("ticket-service /tickets/" + ticketId);
	}

	@Test
	void ticketByBookingGoesToTicketServiceWithTheRewrittenPath() {
		UUID bookingId = UUID.randomUUID();

		assertThat(bodyOf(HttpMethod.GET, "/api/v1/tickets/by-booking/" + bookingId))
				.isEqualTo("ticket-service /tickets/by-booking/" + bookingId);
	}

	@Test
	void scanIsNotExposed() {
		String scan = "/api/v1/tickets/" + UUID.randomUUID() + "/scan";

		statusOf(HttpMethod.POST, scan).isNotFound();
		statusOf(HttpMethod.PUT, scan).isNotFound();
		statusOf(HttpMethod.DELETE, "/api/v1/tickets/" + UUID.randomUUID()).isNotFound();
		assertThat(TICKET_HITS.get()).isZero();
	}

	@Test
	void ticketReadsRequireAToken() {
		client.get().uri("/api/v1/tickets/" + UUID.randomUUID()).exchange().expectStatus().isUnauthorized();
		assertThat(TICKET_HITS.get()).isZero();
	}

	@Test
	void bookingListAndBookingRoutesStillGoToBookingService() {
		assertThat(bodyOf(HttpMethod.GET, "/api/v1/bookings?page=1&size=5"))
				.isEqualTo("booking-service /api/v1/bookings?page=1&size=5");
		UUID bookingId = UUID.randomUUID();
		assertThat(bodyOf(HttpMethod.GET, "/api/v1/bookings/" + bookingId))
				.isEqualTo("booking-service /api/v1/bookings/" + bookingId);
		assertThat(bodyOf(HttpMethod.POST, "/api/v1/bookings/" + bookingId + "/cancel"))
				.isEqualTo("booking-service /api/v1/bookings/" + bookingId + "/cancel");
	}

	@Test
	void catalogRoutesAreUnchanged() {
		assertThat(bodyOf(HttpMethod.GET, "/api/v1/movies")).startsWith("catalog-service ");
		assertThat(bodyOf(HttpMethod.POST, "/graphql")).startsWith("catalog-service ");
	}

	// A fresh subject per call keeps the (deliberately tiny) test rate limit from tripping.
	private WebTestClient.RequestHeadersSpec<?> request(HttpMethod method, String path) {
		return client
				.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(j -> j.subject(UUID.randomUUID().toString())))
				.method(method).uri(path);
	}

	private String bodyOf(HttpMethod method, String path) {
		byte[] body = request(method, path).exchange()
				.expectStatus().isOk()
				.expectBody().returnResult().getResponseBody();
		return new String(body, StandardCharsets.UTF_8);
	}

	private StatusAssertions statusOf(HttpMethod method, String path) {
		return request(method, path).exchange().expectStatus();
	}

	private static HttpServer stub(String name, AtomicInteger hits) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			server.createContext("/", exchange -> {
				hits.incrementAndGet();
				byte[] body = (name + " " + exchange.getRequestURI()).getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
				exchange.close();
			});
			server.start();
			return server;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
