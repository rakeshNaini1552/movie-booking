package com.movie.booking.gateway_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewaySecurityTest {

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
	void noTokenIsUnauthorized() {
		client.get().uri("/api/v1/bookings").exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void garbageBearerTokenIsUnauthorized() {
		client.get().uri("/api/v1/bookings")
				.header("Authorization", "Bearer not-a-jwt")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void validJwtPassesGatewayFilter() {
		client.mutateWith(SecurityMockServerConfigurers.mockJwt())
				.get().uri("/api/v1/bookings").exchange()
				.expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
	}

	@Test
	void catalogPathsWithoutTokenAreUnauthorized() {
		for (String path : new String[] {"/api/v1/movies", "/api/v1/theaters/1/screens", "/api/v1/shows"}) {
			client.get().uri(path).exchange()
					.expectStatus().isUnauthorized();
		}
	}

	@Test
	void catalogPathsWithValidJwtAreRoutedToCatalogService() {
		// Catalog URL is unreachable in tests: a routed request fails with 5xx, an unrouted one would be 404
		for (String path : new String[] {"/api/v1/movies", "/api/v1/theaters/1/screens", "/api/v1/shows"}) {
			client.mutateWith(SecurityMockServerConfigurers.mockJwt())
					.get().uri(path).exchange()
					.expectStatus().value(status -> assertThat(status).isNotIn(401, 404));
		}
	}

	@Test
	void healthIsOpenWithoutToken() {
		client.get().uri("/actuator/health/liveness").exchange()
				.expectStatus().isOk();
	}

}
