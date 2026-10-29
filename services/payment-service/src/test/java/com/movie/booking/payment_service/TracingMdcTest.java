package com.movie.booking.payment_service;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "management.tracing.export.otlp.enabled=false")
@AutoConfigureMockMvc
@AutoConfigureTracing
@Import({TestcontainersConfiguration.class, TracingMdcTest.MdcCapture.class})
class TracingMdcTest {

    static final AtomicReference<String> TRACE_ID = new AtomicReference<>();
    static final AtomicReference<String> SPAN_ID = new AtomicReference<>();

    @TestConfiguration
    static class MdcCapture {
        // Runs innermost, so it sees the MDC while the server observation is still open.
        @Bean
        FilterRegistrationBean<Filter> mdcCaptureFilter() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>((request, response, chain) -> {
                TRACE_ID.set(MDC.get("traceId"));
                SPAN_ID.set(MDC.get("spanId"));
                chain.doFilter(request, response);
            });
            registration.setOrder(Ordered.LOWEST_PRECEDENCE);
            return registration;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void trace_and_span_ids_are_in_the_mdc_during_an_http_request() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"));

        assertThat(TRACE_ID.get()).matches("[0-9a-f]{32}");
        assertThat(SPAN_ID.get()).matches("[0-9a-f]{16}");
    }
}
