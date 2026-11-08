package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovieApiIntegrationTest extends ApiTestBase {

    @Test
    void create_then_get_returns_the_movie_as_active() throws Exception {
        postJson("/api/v1/movies", movieJson("Dune"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Dune"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.durationMinutes").value(120))
                .andExpect(jsonPath("$.releaseDate").value("2030-01-15"));
        UUID id = UUID.fromString(jdbcTemplate.queryForObject("SELECT id::text FROM movie", String.class));

        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Dune"));
    }

    @Test
    void get_unknown_movie_returns_404() throws Exception {
        mockMvc.perform(get("/api/v1/movies/{id}", UUID.randomUUID()).with(TestJwt.user()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void list_filters_by_status_and_title_contains_case_insensitively() throws Exception {
        createMovie("Dune");
        createMovie("Dune: Part Two");
        UUID alien = createMovie("Alien");
        mockMvc.perform(delete("/api/v1/movies/{id}", alien).with(TestJwt.user())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/movies").param("title", "DUNE").with(TestJwt.user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/movies").param("status", "ARCHIVED").with(TestJwt.user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Alien"));
        mockMvc.perform(get("/api/v1/movies").param("title", "%").with(TestJwt.user()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void list_is_paged_with_the_configured_default_size_and_caps_the_requested_size() throws Exception {
        for (int i = 0; i < 3; i++) {
            createMovie("Movie " + i);
        }

        mockMvc.perform(get("/api/v1/movies").param("size", "2").param("sort", "title").with(TestJwt.user()))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        // Default 20 and max 100 come from spring.data.web.pageable.* in application.yml.
        mockMvc.perform(get("/api/v1/movies").with(TestJwt.user()))
                .andExpect(jsonPath("$.size").value(20));
        mockMvc.perform(get("/api/v1/movies").param("size", "100000").with(TestJwt.user()))
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void list_with_an_unknown_sort_property_returns_400() throws Exception {
        mockMvc.perform(get("/api/v1/movies").param("sort", "nope").with(TestJwt.user()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_updates_the_movie_and_keeps_status_when_omitted() throws Exception {
        UUID id = createMovie("Dune");

        mockMvc.perform(put("/api/v1/movies/{id}", id).with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON).content(movieJson("Dune: Part Two")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dune: Part Two"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM movie WHERE id = ?", String.class, id))
                .isEqualTo("Dune: Part Two");
    }

    @Test
    void put_unknown_movie_returns_404() throws Exception {
        mockMvc.perform(put("/api/v1/movies/{id}", UUID.randomUUID()).with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON).content(movieJson("X")))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_is_soft_and_repeatable() throws Exception {
        UUID id = createMovie("Dune");

        mockMvc.perform(delete("/api/v1/movies/{id}", id).with(TestJwt.user())).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/movies/{id}", id).with(TestJwt.user())).andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM movie WHERE id = ?", String.class, id))
                .isEqualTo("ARCHIVED");
    }

    @Test
    void delete_movie_that_has_shows_returns_409_and_keeps_it_active() throws Exception {
        UUID movieId = createMovie("Dune");
        UUID screenId = createScreen(createTheater("PVR", "Pune"), "Screen 1");
        createAndGetId("/api/v1/shows", showJson(movieId, screenId, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));

        mockMvc.perform(delete("/api/v1/movies/{id}", movieId).with(TestJwt.user()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM movie WHERE id = ?", String.class, movieId))
                .isEqualTo("ACTIVE");
    }

    @Test
    void invalid_body_returns_400_and_writes_nothing() throws Exception {
        postJson("/api/v1/movies", "{\"title\": \"\", \"durationMinutes\": -5, \"language\": \"en\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM movie", Integer.class)).isZero();
    }

    @Test
    void requests_without_a_token_are_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/movies")).andExpect(status().isUnauthorized());
    }
}
