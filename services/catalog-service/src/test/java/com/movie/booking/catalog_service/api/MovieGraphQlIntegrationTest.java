package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovieGraphQlIntegrationTest extends ApiTestBase {

    private static final String MOVIE_QUERY = """
            {"query": "query($id: ID!) { movie(id: $id) { id title durationMinutes language } }",
             "variables": {"id": "%s"}}
            """;

    @Test
    void movie_by_id_returns_title_duration_and_language() throws Exception {
        UUID id = createMovie("Dune");

        graphQl(MOVIE_QUERY.formatted(id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.movie.id").value(id.toString()))
                .andExpect(jsonPath("$.data.movie.title").value("Dune"))
                .andExpect(jsonPath("$.data.movie.durationMinutes").value(120))
                .andExpect(jsonPath("$.data.movie.language").value("en"));
    }

    @Test
    void unknown_movie_id_returns_a_not_found_error_and_null_data() throws Exception {
        graphQl(MOVIE_QUERY.formatted(UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data.movie").doesNotExist());
    }

    @Test
    void graphql_without_a_token_returns_401() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON).content(MOVIE_QUERY.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }
}
