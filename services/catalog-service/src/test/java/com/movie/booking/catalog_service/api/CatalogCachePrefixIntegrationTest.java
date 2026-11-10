package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CatalogCachePrefixIntegrationTest extends ApiTestBase {

    @Autowired private StringRedisTemplate redis;

    @Test
    void with_the_default_prefix_a_cached_movie_is_stored_under_a_catalog_key() throws Exception {
        UUID id = createMovie("Dune");

        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user())).andExpect(status().isOk());

        assertThat(redis.keys("*" + id)).containsExactly("catalog:movies::" + id);
    }
}
