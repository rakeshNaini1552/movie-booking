package com.movie.booking.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
@Import(TestcontainersConfiguration.class)
class PrometheusEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheus_without_token_returns_200_with_application_tag_and_http_metrics() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());

        String body = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("application=\"payment-service\"");
        assertThat(body).contains("http_server_requests");
    }

    @Test
    void aggregate_health_without_token_is_still_401() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
    }

    @Test
    void existing_endpoint_still_requires_a_token() throws Exception {
        mockMvc.perform(get("/api/v1/payments/00000000-0000-4000-8000-000000000001")).andExpect(status().isUnauthorized());
    }
}
