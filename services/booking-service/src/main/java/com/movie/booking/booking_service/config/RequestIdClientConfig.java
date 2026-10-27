package com.movie.booking.booking_service.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@Configuration
public class RequestIdClientConfig {

    private static final String HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    @Bean
    public ClientHttpRequestInterceptor requestIdInterceptor() {
        return (request, body, execution) -> {
            String requestId = MDC.get(MDC_KEY);
            if (requestId != null && !requestId.isBlank()) {
                request.getHeaders().set(HEADER, requestId);
            }
            return execution.execute(request, body);
        };
    }
}
