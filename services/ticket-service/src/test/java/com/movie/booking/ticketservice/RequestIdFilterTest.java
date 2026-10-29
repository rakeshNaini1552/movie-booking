package com.movie.booking.ticketservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RequestIdFilterTest {

    private static final String HEADER = "X-Request-Id";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void valid_request_id_is_echoed_in_the_response() throws Exception {
        String header = mockMvc.perform(get("/actuator/health/liveness").header(HEADER, "req-123-abc"))
                .andReturn().getResponse().getHeader(HEADER);

        assertThat(header).isEqualTo("req-123-abc");
    }

    @Test
    void missing_request_id_gets_a_generated_uuid() throws Exception {
        String header = mockMvc.perform(get("/actuator/health/liveness"))
                .andReturn().getResponse().getHeader(HEADER);

        assertThat(UUID.fromString(header)).isNotNull();
    }

    @Test
    void unsafe_request_id_is_replaced_with_a_generated_uuid() throws Exception {
        String header = mockMvc.perform(get("/actuator/health/liveness").header(HEADER, "bad id\twith;chars"))
                .andReturn().getResponse().getHeader(HEADER);

        assertThat(UUID.fromString(header)).isNotNull();
    }
}
