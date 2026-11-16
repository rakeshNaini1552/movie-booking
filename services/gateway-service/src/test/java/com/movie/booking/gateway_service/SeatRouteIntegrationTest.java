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
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Two stub servers stand in for seat-service and catalog-service; each answers with its own name,
// so the response body shows which one the gateway forwarded to.
@SpringBootTest
@Import(TestContainersConfig.class)
class SeatRouteIntegrationTest {

	private static final HttpServer SEAT = stub("seat-service");
	private static final HttpServer CATALOG = stub("catalog-service");

	@DynamicPropertySource
	static void downstreams(DynamicPropertyRegistry registry) {
		registry.add("SEAT_SERVICE_URL", () -> "http://localhost:" + SEAT.getAddress().getPort());
		registry.add("CATALOG_SERVICE_URL", () -> "http://localhost:" + CATALOG.getAddress().getPort());
	}

	@AfterAll
	static void stopStubs() {
		SEAT.stop(0);
		CATALOG.stop(0);
	}

	@Autowired
	ApplicationContext context;

	WebTestClient client;

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToApplicationContext(context)
				.apply(SecurityMockServerConfigurers.springSecurity())
				.build();
	}

	@Test
	void seatMapGoesToSeatService() {
		assertThat(bodyOf(HttpMethod.GET, "/api/v1/shows/" + UUID.randomUUID() + "/seats")).isEqualTo("seat-service");
	}

	@Test
	void showDetailsStillGoToCatalogService() {
		assertThat(bodyOf(HttpMethod.GET, "/api/v1/shows/" + UUID.randomUUID())).isEqualTo("catalog-service");
		assertThat(bodyOf(HttpMethod.GET, "/api/v1/shows")).isEqualTo("catalog-service");
	}

	@Test
	void graphqlStillGoesToCatalogService() {
		assertThat(bodyOf(HttpMethod.POST, "/graphql")).isEqualTo("catalog-service");
	}

	@Test
	void seatMapRequiresAToken() {
		client.get().uri("/api/v1/shows/" + UUID.randomUUID() + "/seats").exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void otherPathsUnderSeatsAreNotRoutedToSeatService() {
		// Deeper paths and writes are not exposed: they fall through to catalog-service, never to seat-service.
		String show = UUID.randomUUID().toString();
		assertThat(bodyOf(HttpMethod.GET, "/api/v1/shows/" + show + "/seats/holds")).isEqualTo("catalog-service");
		assertThat(bodyOf(HttpMethod.POST, "/api/v1/shows/" + show + "/seats")).isEqualTo("catalog-service");
		assertThat(bodyOf(HttpMethod.DELETE, "/api/v1/shows/" + show + "/seats")).isEqualTo("catalog-service");
	}

	// A fresh subject per call keeps the (deliberately tiny) test rate limit from tripping.
	private String bodyOf(HttpMethod method, String path) {
		byte[] body = client
				.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(j -> j.subject(UUID.randomUUID().toString())))
				.method(method).uri(path).exchange()
				.expectStatus().isOk()
				.expectBody().returnResult().getResponseBody();
		return new String(body, StandardCharsets.UTF_8);
	}

	private static HttpServer stub(String name) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			server.createContext("/", exchange -> {
				byte[] body = name.getBytes(StandardCharsets.UTF_8);
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
