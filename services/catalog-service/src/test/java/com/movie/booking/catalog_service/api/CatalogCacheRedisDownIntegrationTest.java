package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Stops the Redis container, so it gets its own context that is thrown away afterwards.
@SpringBootTest(properties = "catalog.cache.ttl=PT9M")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogCacheRedisDownIntegrationTest extends ApiTestBase {

    @Autowired private GenericContainer<?> redis;

    @Test
    void reads_and_writes_still_work_from_the_database_when_redis_is_down() throws Exception {
        UUID cached = createMovie("Dune");
        mockMvc.perform(get("/api/v1/movies/{id}", cached).with(TestJwt.user())).andExpect(status().isOk());
        UUID uncached = createMovie("Alien");

        redis.stop();

        mockMvc.perform(get("/api/v1/movies/{id}", cached).with(TestJwt.user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Dune"));
        mockMvc.perform(get("/api/v1/movies/{id}", uncached).with(TestJwt.user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Alien"));
        mockMvc.perform(put("/api/v1/movies/{id}", cached).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON).content(movieJson("Dune 2")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/movies/{id}", cached).with(TestJwt.user()))
                .andExpect(jsonPath("$.title").value("Dune 2"));
    }
}
