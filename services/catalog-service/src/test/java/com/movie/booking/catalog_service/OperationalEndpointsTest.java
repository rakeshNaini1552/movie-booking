package com.movie.booking.catalog_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
@Import(TestContainersConfig.class)
class OperationalEndpointsTest {

    private static final String HEADER = "X-Request-Id";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void liveness_and_readiness_are_open_and_up() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void aggregate_health_requires_a_token_and_the_api_stays_protected() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health").with(TestJwt.user())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/movies")).andExpect(status().isUnauthorized());
    }

    @Test
    void prometheus_is_open_and_tagged_with_the_application_name() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());

        String body = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("application=\"catalog-service\"");
        assertThat(body).contains("http_server_requests");
    }

    @Test
    void valid_request_id_is_echoed_and_missing_or_unsafe_ones_are_replaced() throws Exception {
        assertThat(mockMvc.perform(get("/actuator/health/liveness").header(HEADER, "req-123-abc"))
                .andReturn().getResponse().getHeader(HEADER)).isEqualTo("req-123-abc");

        String generated = mockMvc.perform(get("/actuator/health/liveness"))
                .andReturn().getResponse().getHeader(HEADER);
        assertThat(UUID.fromString(generated)).isNotNull();

        String replaced = mockMvc.perform(get("/actuator/health/liveness").header(HEADER, "bad id\twith;chars"))
                .andReturn().getResponse().getHeader(HEADER);
        assertThat(UUID.fromString(replaced)).isNotNull();
    }
}
