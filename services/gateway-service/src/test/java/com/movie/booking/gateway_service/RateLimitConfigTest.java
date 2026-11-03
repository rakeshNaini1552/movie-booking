package com.movie.booking.gateway_service;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;

class RateLimitConfigTest {

	private final KeyResolver resolver = new RateLimitConfig().userKeyResolver();

	@Test
	void keyIsJwtSubject() {
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-1")
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
		SecurityContextImpl context = new SecurityContextImpl(new JwtAuthenticationToken(jwt, List.of()));

		StepVerifier.create(resolver.resolve(exchange("10.0.0.5"))
						.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
				.expectNext("user-1")
				.verifyComplete();
	}

	@Test
	void keyFallsBackToRemoteAddressWithoutPrincipal() {
		StepVerifier.create(resolver.resolve(exchange("10.0.0.5")))
				.expectNext("10.0.0.5")
				.verifyComplete();
	}

	private static MockServerWebExchange exchange(String remoteIp) {
		ServerHttpRequest request = MockServerHttpRequest.get("/api/v1/bookings")
				.remoteAddress(new InetSocketAddress(remoteIp, 12345))
				.build();
		return MockServerWebExchange.from((MockServerHttpRequest) request);
	}

}
