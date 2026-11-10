package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.ObjectProvider;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = "catalog.cache.enabled=false")
class CatalogCacheDisabledIntegrationTest extends ApiTestBase {

    @Autowired private ObjectProvider<CacheManager> cacheManager;

    @Test
    void with_the_cache_switched_off_every_read_goes_to_the_database() throws Exception {
        UUID id = createMovie("Dune");
        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user())).andExpect(jsonPath("$.title").value("Dune"));

        jdbcTemplate.update("UPDATE movie SET title = 'Changed' WHERE id = ?", id);

        assertThat(cacheManager.getIfAvailable()).isNull();
        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user())).andExpect(jsonPath("$.title").value("Changed"));
    }
}
