package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ShowOverlapConcurrencyIntegrationTest extends ApiTestBase {

    private static final int ROUNDS = 15;

    private static final String INSERT = "INSERT INTO show (id, movie_id, screen_id, starts_at, ends_at, base_price, status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?::timestamptz, ?::timestamptz, 10, ?, now(), now())";

    private int createShowStatus(UUID movie, UUID screen, String startsAt, String endsAt) throws Exception {
        return mockMvc.perform(post("/api/v1/shows").with(TestJwt.admin()).contentType(MediaType.APPLICATION_JSON)
                .content(showJson(movie, screen, startsAt, endsAt))).andReturn().getResponse().getStatus();
    }

    @Test
    void two_simultaneous_overlapping_creates_on_one_screen_let_exactly_one_succeed() throws Exception {
        UUID movie = createMovie("Dune");
        UUID theater = createTheater("PVR", "Pune");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                UUID screen = createScreen(theater, "Screen " + round);
                CountDownLatch go = new CountDownLatch(1);
                // Different start times, so only the overlap rule (not the same-start unique index) can reject one.
                List<Callable<Integer>> calls = List.of(
                        () -> { go.await(); return createShowStatus(movie, screen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"); },
                        () -> { go.await(); return createShowStatus(movie, screen, "2030-02-01T19:00:00Z", "2030-02-01T21:00:00Z"); });
                List<Future<Integer>> futures = new ArrayList<>();
                calls.forEach(c -> futures.add(pool.submit(c)));
                go.countDown();
                List<Integer> statuses = new ArrayList<>();
                for (Future<Integer> f : futures) {
                    statuses.add(f.get());
                }
                assertThat(statuses).as("round %d", round).containsExactlyInAnyOrder(201, 409);
                assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM show WHERE screen_id = ?", Integer.class, screen))
                        .isEqualTo(1);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void back_to_back_shows_and_a_slot_freed_by_cancelling_are_still_allowed() throws Exception {
        UUID movie = createMovie("Dune");
        UUID screen = createScreen(createTheater("PVR", "Pune"), "Screen 1");

        assertThat(createShowStatus(movie, screen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z")).isEqualTo(201);
        assertThat(createShowStatus(movie, screen, "2030-02-01T20:00:00Z", "2030-02-01T22:00:00Z")).isEqualTo(201);
        assertThat(createShowStatus(movie, screen, "2030-02-01T19:59:00Z", "2030-02-01T20:01:00Z")).isEqualTo(409);

        jdbcTemplate.update("UPDATE show SET status = 'CANCELLED' WHERE starts_at = '2030-02-01T18:00:00Z'");
        assertThat(createShowStatus(movie, screen, "2030-02-01T18:30:00Z", "2030-02-01T19:30:00Z")).isEqualTo(201);
    }

    @Test
    void the_database_itself_rejects_an_overlap_but_not_back_to_back_or_cancelled_rows() throws Exception {
        UUID movie = createMovie("Dune");
        UUID screen = createScreen(createTheater("PVR", "Pune"), "Screen 1");
        jdbcTemplate.update(INSERT, UUID.randomUUID(), movie, screen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z", "SCHEDULED");

        jdbcTemplate.update(INSERT, UUID.randomUUID(), movie, screen, "2030-02-01T20:00:00Z", "2030-02-01T21:00:00Z", "SCHEDULED");
        jdbcTemplate.update(INSERT, UUID.randomUUID(), movie, screen, "2030-02-01T18:30:00Z", "2030-02-01T19:30:00Z", "CANCELLED");
        assertThatThrownBy(() -> jdbcTemplate.update(INSERT, UUID.randomUUID(), movie, screen,
                "2030-02-01T19:00:00Z", "2030-02-01T20:30:00Z", "SCHEDULED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("show_screen_no_overlap");
    }

    @Test
    void an_update_that_moves_a_show_into_an_occupied_slot_returns_409() throws Exception {
        UUID movie = createMovie("Dune");
        UUID screen = createScreen(createTheater("PVR", "Pune"), "Screen 1");
        createShowStatus(movie, screen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z");
        UUID second = createAndGetId("/api/v1/shows", showJson(movie, screen, "2030-02-01T20:00:00Z", "2030-02-01T22:00:00Z"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/shows/{id}", second)
                        .with(TestJwt.admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(movie, screen, "2030-02-01T19:00:00Z", "2030-02-01T21:00:00Z")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
    }
}
