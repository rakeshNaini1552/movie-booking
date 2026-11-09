package com.movie.booking.catalog_service.api;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CatalogGraphQlNestedIntegrationTest extends ApiTestBase {

    private static final int MOVIES = 20;

    // movies + shows + screens + theaters, however many movies come back.
    private static final int MAX_STATEMENTS = 4;

    private static final String NESTED_QUERY = """
            {"query": "{ movies(size: 50) { id title shows { id startsAt status screen { id name theater { id name city } } } } }"}
            """;

    @Autowired private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void statistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    // Two theaters with two screens each; every movie gets one show per theater so shows, screens and theaters repeat.
    private void createCatalog(int movies) throws Exception {
        UUID screenA = createScreen(createTheater("PVR", "Pune"), "Screen 1");
        UUID screenB = createScreen(createTheater("INOX", "Mumbai"), "Screen 1");
        for (int i = 0; i < movies; i++) {
            UUID movie = createMovie("Movie %02d".formatted(i));
            // Different start times so the order of a movie's shows (by startsAt) is not a tie.
            createAndGetId("/api/v1/shows", showJson(movie, screenA,
                    "2030-02-01T%02d:00:00Z".formatted(i), "2030-02-01T%02d:20:00Z".formatted(i)));
            createAndGetId("/api/v1/shows", showJson(movie, screenB,
                    "2030-02-01T%02d:30:00Z".formatted(i), "2030-02-01T%02d:50:00Z".formatted(i)));
        }
    }

    @Test
    void nested_query_returns_shows_screens_and_theaters() throws Exception {
        createCatalog(2);

        graphQl(NESTED_QUERY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.movies.length()").value(2))
                .andExpect(jsonPath("$.data.movies[0].title").value("Movie 00"))
                .andExpect(jsonPath("$.data.movies[0].shows.length()").value(2))
                .andExpect(jsonPath("$.data.movies[0].shows[0].screen.name").value("Screen 1"))
                .andExpect(jsonPath("$.data.movies[0].shows[0].screen.theater.name").value("PVR"))
                .andExpect(jsonPath("$.data.movies[0].shows[1].screen.theater.name").value("INOX"));
    }

    @Test
    void nested_query_runs_a_fixed_number_of_statements_regardless_of_the_movie_count() throws Exception {
        createCatalog(MOVIES);
        statistics.clear();

        graphQl(NESTED_QUERY)
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.movies.length()").value(MOVIES));

        assertThat(statistics.getPrepareStatementCount()).as("SQL statements").isLessThanOrEqualTo(MAX_STATEMENTS);
    }

    @Test
    void movies_query_filters_pages_and_caps_the_page_size() throws Exception {
        createCatalog(3);
        jdbcTemplate.update("UPDATE movie SET status = 'ARCHIVED' WHERE title = 'Movie 02'");

        graphQl("{\"query\": \"{ movies(status: \\\"ARCHIVED\\\") { title } }\"}")
                .andExpect(jsonPath("$.data.movies.length()").value(1))
                .andExpect(jsonPath("$.data.movies[0].title").value("Movie 02"));
        graphQl("{\"query\": \"{ movies(titleContains: \\\"movie\\\", page: 1, size: 2) { title } }\"}")
                .andExpect(jsonPath("$.data.movies.length()").value(1))
                .andExpect(jsonPath("$.data.movies[0].title").value("Movie 02"));
        graphQl("{\"query\": \"{ movies(status: \\\"NOPE\\\") { title } }\"}")
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
        graphQl("{\"query\": \"{ movies(size: 0) { title } }\"}")
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }
}
