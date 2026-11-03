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
	void healthIsOpenWithoutToken() {
		client.get().uri("/actuator/health").exchange()
				.expectStatus().isOk();
	}

}
