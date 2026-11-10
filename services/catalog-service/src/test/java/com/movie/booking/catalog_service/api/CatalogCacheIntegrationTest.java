package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import com.movie.booking.catalog_service.ShowStatus;
import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.config.CacheNames;
import com.movie.booking.catalog_service.dto.MovieRequest;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.service.MovieService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CatalogCacheIntegrationTest extends ApiTestBase {

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private CacheManager cacheManager;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private MovieService movieService;

    private Statistics statistics;

    @BeforeEach
    void statistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private void getMovie(UUID id, String expectedTitle) throws Exception {
        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value(expectedTitle));
    }

    @Test
    void second_read_of_the_same_id_is_a_hit_and_runs_no_sql() throws Exception {
        UUID id = createMovie("Dune");
        getMovie(id, "Dune");
        statistics.clear();

        getMovie(id, "Dune");
        getMovie(id, "Dune");

        assertThat(statistics.getPrepareStatementCount()).isZero();
        assertThat(meterRegistry.find("cache.gets").tag("result", "hit").functionCounters()
                .stream().mapToDouble(c -> c.count()).sum()).isPositive();
    }

    @Test
    void a_hit_is_served_from_the_cache_and_an_update_through_the_api_evicts_it() throws Exception {
        UUID id = createMovie("Dune");
        getMovie(id, "Dune");

        // Changed behind the cache's back: the cached copy is still served.
        jdbcTemplate.update("UPDATE movie SET title = 'Changed in the database' WHERE id = ?", id);
        getMovie(id, "Dune");

        mockMvc.perform(put("/api/v1/movies/{id}", id).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON).content(movieJson("Dune 2")))
                .andExpect(status().isOk());
        getMovie(id, "Dune 2");
    }

    // Cache writes are applied immediately, so an update or archive is visible to the very next read.
    // With the asynchronous default of Spring Data Redis the eviction can still be pending and the read serves the stale entry.
    private static final int ITERATIONS = 60;

    @Test
    void an_update_is_visible_to_the_very_next_read() throws Exception {
        UUID id = createMovie("Dune 0");
        getMovie(id, "Dune 0");

        for (int i = 1; i <= ITERATIONS; i++) {
            mockMvc.perform(put("/api/v1/movies/{id}", id).with(TestJwt.admin())
                            .contentType(MediaType.APPLICATION_JSON).content(movieJson("Dune " + i)))
                    .andExpect(status().isOk());
            getMovie(id, "Dune " + i);
        }
    }

    @Test
    void an_archive_is_visible_to_the_very_next_read() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            UUID id = createMovie("Dune " + i);
            getMovie(id, "Dune " + i);

            mockMvc.perform(delete("/api/v1/movies/{id}", id).with(TestJwt.admin())).andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user()))
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));
        }
    }

    // The same check without the HTTP layer: the read follows the write within microseconds, so a pending
    // asynchronous eviction is caught reliably. Through MockMvc the gap is usually long enough to hide it.
    @Test
    void a_service_update_is_visible_to_the_read_straight_after() {
        UUID id = movieService.create(movieRequest("Dune 0")).id();
        assertThat(movieService.get(id).title()).isEqualTo("Dune 0");

        for (int i = 1; i <= ITERATIONS; i++) {
            movieService.update(id, movieRequest("Dune " + i));
            assertThat(movieService.get(id).title()).as("read after update %d", i).isEqualTo("Dune " + i);
        }
    }

    @Test
    void a_service_archive_is_visible_to_the_read_straight_after() {
        for (int i = 0; i < ITERATIONS; i++) {
            UUID id = movieService.create(movieRequest("Dune " + i)).id();
            assertThat(movieService.get(id).status()).isEqualTo(MovieStatus.ACTIVE);

            movieService.archive(id);

            assertThat(movieService.get(id).status()).as("read after archive %d", i).isEqualTo(MovieStatus.ARCHIVED);
        }
    }

    private static MovieRequest movieRequest(String title) {
        return new MovieRequest(title, "d", 120, "en", "PG-13", LocalDate.parse("2030-01-15"), null);
    }

    @Test
    void archive_evicts_the_movie() throws Exception {
        UUID id = createMovie("Dune");
        getMovie(id, "Dune");

        mockMvc.perform(delete("/api/v1/movies/{id}", id).with(TestJwt.admin())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void theater_update_cancel_show_and_screen_changes_evict_their_entries() throws Exception {
        UUID movie = createMovie("Dune");
        UUID theater = createTheater("PVR", "Pune");
        UUID screen = createScreen(theater, "Screen 1");
        UUID show = createAndGetId("/api/v1/shows", showJson(movie, screen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));
        mockMvc.perform(get("/api/v1/theaters/{id}", theater).with(TestJwt.user())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theater, screen).with(TestJwt.user())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/shows/{id}", show).with(TestJwt.user())).andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/theaters/{id}", theater).with(TestJwt.admin())
                .contentType(MediaType.APPLICATION_JSON).content(theaterJson("PVR Icon", "Pune"))).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/theaters/{t}/screens/{s}", theater, screen).with(TestJwt.admin())
                .contentType(MediaType.APPLICATION_JSON).content(screenJson("Screen One"))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", show).with(TestJwt.admin())).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/theaters/{id}", theater).with(TestJwt.user())).andExpect(jsonPath("$.name").value("PVR Icon"));
        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theater, screen).with(TestJwt.user())).andExpect(jsonPath("$.name").value("Screen One"));
        mockMvc.perform(get("/api/v1/shows/{id}", show).with(TestJwt.user())).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void deleting_a_screen_or_a_theater_evicts_the_cached_entries() throws Exception {
        UUID theater = createTheater("PVR", "Pune");
        UUID screen1 = createScreen(theater, "Screen 1");
        UUID screen2 = createScreen(theater, "Screen 2");
        for (UUID s : new UUID[]{screen1, screen2}) {
            mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theater, s).with(TestJwt.user())).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/theaters/{id}", theater).with(TestJwt.user())).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/theaters/{t}/screens/{s}", theater, screen1).with(TestJwt.admin())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theater, screen1).with(TestJwt.user())).andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/theaters/{id}", theater).with(TestJwt.admin())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/theaters/{id}", theater).with(TestJwt.user())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theater, screen2).with(TestJwt.user())).andExpect(status().isNotFound());
    }

    @Test
    void a_screen_id_under_the_wrong_theater_is_not_served_from_another_theaters_entry() throws Exception {
        UUID theater = createTheater("PVR", "Pune");
        UUID other = createTheater("INOX", "Mumbai");
        UUID screen = createScreen(theater, "Screen 1");
        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theater, screen).with(TestJwt.user())).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", other, screen).with(TestJwt.user())).andExpect(status().isNotFound());
    }

    @Test
    void lists_are_not_cached() throws Exception {
        createMovie("Dune");
        mockMvc.perform(get("/api/v1/movies").with(TestJwt.user())).andExpect(jsonPath("$.totalElements").value(1));

        jdbcTemplate.update("UPDATE movie SET title = 'Changed'");

        mockMvc.perform(get("/api/v1/movies").with(TestJwt.user())).andExpect(jsonPath("$.content[0].title").value("Changed"));
    }

    @Test
    void cached_json_round_trips_instants_dates_enums_and_decimals() {
        Instant now = Instant.parse("2030-01-15T10:15:30.123456Z");
        MovieResponse movie = new MovieResponse(UUID.randomUUID(), "Dune", null, 155, "en", "PG-13",
                LocalDate.parse("2030-01-15"), MovieStatus.ARCHIVED, now, now);
        ShowResponse show = new ShowResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now, now.plusSeconds(7200),
                new BigDecimal("12.50"), ShowStatus.CANCELLED, now, now);

        cacheManager.getCache(CacheNames.MOVIES).put(movie.id(), movie);
        cacheManager.getCache(CacheNames.SHOWS).put(show.id(), show);

        assertThat(cacheManager.getCache(CacheNames.MOVIES).get(movie.id(), MovieResponse.class)).isEqualTo(movie);
        assertThat(cacheManager.getCache(CacheNames.SHOWS).get(show.id(), ShowResponse.class)).isEqualTo(show);
    }
}
