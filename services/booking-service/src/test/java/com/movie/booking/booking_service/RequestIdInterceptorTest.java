package com.movie.booking.booking_service;

import com.movie.booking.booking_service.config.RequestIdClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdInterceptorTest {

    private static final String HEADER = "X-Request-Id";

    private final ClientHttpRequestInterceptor interceptor = new RequestIdClientConfig().requestIdInterceptor();
    private final HttpRequest request = mock(HttpRequest.class);
    private final HttpHeaders headers = new HttpHeaders();
    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    private final ClientHttpResponse response = mock(ClientHttpResponse.class);
    private final byte[] body = new byte[0];

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private void intercept() throws IOException {
        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(request, body)).thenReturn(response);
        assertThat(interceptor.intercept(request, body, execution)).isSameAs(response);
        verify(execution).execute(request, body);
    }

    @Test
    void sets_the_header_when_the_mdc_has_a_request_id() throws IOException {
        MDC.put("requestId", "req-123");

        intercept();

        assertThat(headers.getFirst(HEADER)).isEqualTo("req-123");
    }

    @Test
    void leaves_the_header_absent_when_the_mdc_is_empty() throws IOException {
        intercept();

        assertThat(headers.containsHeader(HEADER)).isFalse();
    }

    @Test
    void leaves_the_header_absent_when_the_request_id_is_blank() throws IOException {
        MDC.put("requestId", "  ");

        intercept();

        assertThat(headers.containsHeader(HEADER)).isFalse();
    }
}
