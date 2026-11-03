package com.movie.booking.gateway_service;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String incoming = exchange.getRequest().getHeaders().getFirst(RequestId.HEADER);
		String requestId = incoming != null && RequestId.SAFE_ID.matcher(incoming).matches()
				? incoming : UUID.randomUUID().toString();

		ServerHttpRequest request = exchange.getRequest().mutate()
				.headers(h -> h.set(RequestId.HEADER, requestId))
				.build();
		exchange.getAttributes().put(RequestId.ATTRIBUTE, requestId);
		// Set at commit so it replaces the copy the downstream response brings back instead of duplicating it
		exchange.getResponse().beforeCommit(() -> {
			exchange.getResponse().getHeaders().set(RequestId.HEADER, requestId);
			return Mono.empty();
		});

		return chain.filter(exchange.mutate().request(request).build())
				.contextWrite(ctx -> ctx.put(RequestId.ATTRIBUTE, requestId));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
