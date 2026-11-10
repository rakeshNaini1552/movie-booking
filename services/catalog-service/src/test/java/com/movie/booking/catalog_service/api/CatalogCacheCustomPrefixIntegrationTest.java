package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.TestJwt;
import com.movie.booking.catalog_service.dto.MovieResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "catalog.cache.prefix=catalog-stage:")
class CatalogCacheCustomPrefixIntegrationTest extends ApiTestBase {

    @Autowired private StringRedisTemplate redis;
    @Autowired private JsonMapper jsonMapper;

    @Test
    void a_configured_prefix_replaces_the_default_in_the_key() throws Exception {
        UUID id = createMovie("Dune");

        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user())).andExpect(status().isOk());

        assertThat(redis.keys("*" + id)).containsExactly("catalog-stage:movies::" + id);
    }

    @Test
    void an_entry_under_the_default_prefix_is_a_miss_and_the_read_goes_to_the_database() throws Exception {
        UUID id = createMovie("Dune");
        Instant now = Instant.parse("2030-01-15T10:15:30Z");
        String planted = jsonMapper.writeValueAsString(new MovieResponse(id, "Planted by another environment", null, 120,
                "en", "PG-13", LocalDate.parse("2030-01-15"), MovieStatus.ACTIVE, now, now));
        redis.opsForValue().set("catalog:movies::" + id, planted);

        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Dune"));

        assertThat(redis.hasKey("catalog-stage:movies::" + id)).isTrue();
        assertThat(redis.opsForValue().get("catalog:movies::" + id)).isEqualTo(planted);
    }
}
