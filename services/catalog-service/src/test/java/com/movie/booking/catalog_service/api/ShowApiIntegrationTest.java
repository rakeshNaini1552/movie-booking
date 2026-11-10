package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShowApiIntegrationTest extends ApiTestBase {

    private UUID movieId;
    private UUID theaterId;
    private UUID screenId;

    @BeforeEach
    void createFixtures() throws Exception {
        movieId = createMovie("Dune");
        theaterId = createTheater("PVR", "Pune");
        screenId = createScreen(theaterId, "Screen 1");
    }

    private UUID createShow(String startsAt, String endsAt) throws Exception {
        return createAndGetId("/api/v1/shows", showJson(movieId, screenId, startsAt, endsAt));
    }

    @Test
    void create_then_get_returns_a_scheduled_show() throws Exception {
        UUID id = createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");

        mockMvc.perform(get("/api/v1/shows/{id}", id).with(TestJwt.user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(movieId.toString()))
                .andExpect(jsonPath("$.screenId").value(screenId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.basePrice").value(12.50));
    }

    @Test
    void overlapping_show_on_the_same_screen_returns_409_and_back_to_back_is_allowed() throws Exception {
        createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");

        postJson("/api/v1/shows", showJson(movieId, screenId, "2030-02-01T19:00:00Z", "2030-02-01T21:00:00Z"))
                .andExpect(status().isConflict());
        postJson("/api/v1/shows", showJson(movieId, screenId, "2030-02-01T17:00:00Z", "2030-02-01T18:30:00Z"))
                .andExpect(status().isConflict());
        createShow("2030-02-01T20:00:00Z", "2030-02-01T22:00:00Z");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM show", Integer.class)).isEqualTo(2);
    }

    @Test
    void overlap_on_a_different_screen_is_fine() throws Exception {
        createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");
        UUID otherScreen = createScreen(theaterId, "Screen 2");

        createAndGetId("/api/v1/shows", showJson(movieId, otherScreen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));
    }

    @Test
    void cancelled_show_no_longer_blocks_the_slot() throws Exception {
        UUID first = createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", first).with(TestJwt.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        createShow("2030-02-01T18:30:00Z", "2030-02-01T20:30:00Z");
    }

    @Test
    void a_new_show_can_reuse_the_exact_start_of_a_cancelled_show() throws Exception {
        UUID first = createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", first).with(TestJwt.admin())).andExpect(status().isOk());

        createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM show", Integer.class)).isEqualTo(2);
    }

    @Test
    void the_database_rejects_two_active_shows_with_the_same_screen_and_start() {
        UUID screen = screenId;
        String insert = "INSERT INTO show (id, movie_id, screen_id, starts_at, ends_at, base_price, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, '2030-02-01T18:00:00Z', '2030-02-01T20:00:00Z', 10, ?, now(), now())";
        jdbcTemplate.update(insert, UUID.randomUUID(), movieId, screen, "SCHEDULED");
        jdbcTemplate.update(insert, UUID.randomUUID(), movieId, screen, "CANCELLED");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> jdbcTemplate.update(insert, UUID.randomUUID(), movieId, screen, "SCHEDULED"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void cancel_twice_is_idempotent_and_unknown_show_is_404() throws Exception {
        UUID id = createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");

        mockMvc.perform(post("/api/v1/shows/{id}/cancel", id).with(TestJwt.admin())).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", id).with(TestJwt.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", UUID.randomUUID()).with(TestJwt.admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void end_before_start_returns_400_and_writes_nothing() throws Exception {
        postJson("/api/v1/shows", showJson(movieId, screenId, "2030-02-01T20:00:00Z", "2030-02-01T18:00:00Z"))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM show", Integer.class)).isZero();
    }

    @Test
    void unknown_movie_or_screen_returns_404_and_archived_movie_returns_409() throws Exception {
        postJson("/api/v1/shows", showJson(UUID.randomUUID(), screenId, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"))
                .andExpect(status().isNotFound());
        postJson("/api/v1/shows", showJson(movieId, UUID.randomUUID(), "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"))
                .andExpect(status().isNotFound());

        jdbcTemplate.update("UPDATE movie SET status = 'ARCHIVED' WHERE id = ?", movieId);
        postJson("/api/v1/shows", showJson(movieId, screenId, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"))
                .andExpect(status().isConflict());
    }

    @Test
    void put_updates_the_show_and_ignores_its_own_slot_in_the_overlap_check() throws Exception {
        UUID id = createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");

        mockMvc.perform(put("/api/v1/shows/{id}", id).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(movieId, screenId, "2030-02-01T18:00:00Z", "2030-02-01T21:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsAt").value("2030-02-01T21:00:00Z"));
    }

    @Test
    void put_that_would_overlap_another_show_returns_409() throws Exception {
        createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");
        UUID second = createShow("2030-02-01T20:00:00Z", "2030-02-01T22:00:00Z");

        mockMvc.perform(put("/api/v1/shows/{id}", second).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(movieId, screenId, "2030-02-01T19:00:00Z", "2030-02-01T22:00:00Z")))
                .andExpect(status().isConflict());
    }

    @Test
    void put_of_a_cancelled_show_returns_409() throws Exception {
        UUID id = createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", id).with(TestJwt.admin())).andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/shows/{id}", id).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(movieId, screenId, "2030-03-01T18:00:00Z", "2030-03-01T20:00:00Z")))
                .andExpect(status().isConflict());
    }

    @Test
    void list_filters_by_movie_theater_and_time_window() throws Exception {
        createShow("2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");
        createShow("2030-02-02T18:00:00Z", "2030-02-02T20:00:00Z");
        UUID otherMovie = createMovie("Alien");
        UUID otherScreen = createScreen(createTheater("INOX", "Mumbai"), "Screen 1");
        createAndGetId("/api/v1/shows",
                showJson(otherMovie, otherScreen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));

        mockMvc.perform(get("/api/v1/shows").with(TestJwt.user()))
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/v1/shows").param("movieId", otherMovie.toString()).with(TestJwt.user()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/shows").param("theaterId", theaterId.toString()).with(TestJwt.user()))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/shows").param("theaterId", theaterId.toString())
                        .param("from", "2030-02-02T00:00:00Z").param("to", "2030-02-03T00:00:00Z")
                        .with(TestJwt.user()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].startsAt").value("2030-02-02T18:00:00Z"));
        mockMvc.perform(get("/api/v1/shows").param("size", "2").param("sort", "startsAt,desc").with(TestJwt.user()))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void invalid_instant_filter_returns_400() throws Exception {
        mockMvc.perform(get("/api/v1/shows").param("from", "yesterday").with(TestJwt.user()))
                .andExpect(status().isBadRequest());
    }
}
