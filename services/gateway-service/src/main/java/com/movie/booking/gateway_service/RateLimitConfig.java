package com.movie.booking.gateway_service;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {

	@Bean
	KeyResolver userKeyResolver() {
		return exchange -> ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication)
				.filter(Authentication::isAuthenticated)
				.map(Authentication::getPrincipal)
				.ofType(Jwt.class)
				.map(Jwt::getSubject)
				.switchIfEmpty(Mono.fromSupplier(() -> remoteAddress(exchange)));
	}

	private static String remoteAddress(ServerWebExchange exchange) {
		InetSocketAddress address = exchange.getRequest().getRemoteAddress();
		if (address == null || address.getAddress() == null) {
			return "unknown";
		}
		return address.getAddress().getHostAddress();
	}

}
