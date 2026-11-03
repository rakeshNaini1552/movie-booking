package com.movie.booking.gateway_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class RateLimitIntegrationTest {

	@Autowired
	ApplicationContext context;

	@Autowired
	ReactiveStringRedisTemplate redis;

	WebTestClient client;

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToApplicationContext(context)
				.apply(SecurityMockServerConfigurers.springSecurity())
				.build();
	}

	@Test
	void thirdRapidRequestOfOneUserIsRejected() {
		String subject = UUID.randomUUID().toString();

		assertThat(statusOf(subject)).isNotEqualTo(429);
		assertThat(statusOf(subject)).isNotEqualTo(429);

		client.mutateWith(jwtOf(subject)).get().uri("/api/v1/bookings").exchange()
				.expectStatus().isEqualTo(429)
				.expectHeader().exists("X-RateLimit-Remaining")
				.expectHeader().exists("X-RateLimit-Burst-Capacity");
	}

	@Test
	void otherUserIsNotLimitedByFirstUser() {
		String heavyUser = UUID.randomUUID().toString();
		for (int i = 0; i < 4; i++) {
			statusOf(heavyUser);
		}

		assertThat(statusOf(UUID.randomUUID().toString())).isNotEqualTo(429);
	}

	@Test
	void unauthenticatedRequestsAreRejectedAndConsumeNoTokens() {
		Long keysBefore = rateLimiterKeyCount();

		for (int i = 0; i < 5; i++) {
			client.get().uri("/api/v1/bookings").exchange().expectStatus().isUnauthorized();
		}

		assertThat(rateLimiterKeyCount()).isEqualTo(keysBefore);
	}

	private Long rateLimiterKeyCount() {
		return redis.keys("request_rate_limiter.*").count().block();
	}

	private int statusOf(String subject) {
		return client.mutateWith(jwtOf(subject)).get().uri("/api/v1/bookings").exchange()
				.returnResult(String.class).getStatus().value();
	}

	private static SecurityMockServerConfigurers.JwtMutator jwtOf(String subject) {
		return SecurityMockServerConfigurers.mockJwt().jwt(j -> j.subject(subject));
	}

}
