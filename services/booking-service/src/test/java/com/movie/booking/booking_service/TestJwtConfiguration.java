package com.movie.booking.booking_service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

@TestConfiguration(proxyBeanMethods = false)
public class TestJwtConfiguration {

	// Replaces the issuer-uri decoder so no test ever contacts Keycloak. Authenticated requests use the
	// jwt() post-processor, which bypasses decoding; a real bearer token is always rejected.
	@Bean
	JwtDecoder jwtDecoder() {
		return token -> {
			throw new BadJwtException("No tokens are accepted in tests");
		};
	}

	// Replaces the issuer-uri registration, whose discovery call would contact Keycloak at startup.
	@Bean
	ClientRegistrationRepository clientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("booking-service")
				.clientId("booking-service")
				.clientSecret("test-client-secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.tokenUri("http://localhost:0/token")
				.build());
	}

	// Hands out a fixed token so real RestClients never call the token endpoint.
	@Bean
	@Primary
	OAuth2AuthorizedClientManager testAuthorizedClientManager(ClientRegistrationRepository registrations) {
		return request -> {
			Instant now = Instant.now();
			OAuth2AccessToken token = new OAuth2AccessToken(
					OAuth2AccessToken.TokenType.BEARER, "test-service-token", now, now.plusSeconds(300));
			return new OAuth2AuthorizedClient(registrations.findByRegistrationId("booking-service"),
					"booking-service", token);
		};
	}

}
