package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestContainersConfig;
import com.movie.booking.catalog_service.TestJwt;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
abstract class ApiTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE show, screen, theater, movie");
    }

    protected ResultActions postJson(String path, String body, Object... uriVars) throws Exception {
        return mockMvc.perform(post(path, uriVars).with(TestJwt.user())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected UUID createAndGetId(String path, String body, Object... uriVars) throws Exception {
        String json = postJson(path, body, uriVars).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    protected UUID createMovie(String title) throws Exception {
        return createAndGetId("/api/v1/movies", movieJson(title));
    }

    protected UUID createTheater(String name, String city) throws Exception {
        return createAndGetId("/api/v1/theaters", theaterJson(name, city));
    }

    protected UUID createScreen(UUID theaterId, String name) throws Exception {
        return createAndGetId("/api/v1/theaters/{id}/screens", screenJson(name), theaterId);
    }

    protected static String movieJson(String title) {
        return """
                {"title": "%s", "description": "d", "durationMinutes": 120, "language": "en",
                 "rating": "PG-13", "releaseDate": "2030-01-15"}
                """.formatted(title);
    }

    protected static String theaterJson(String name, String city) {
        return """
                {"name": "%s", "city": "%s", "address": "1 Main St"}
                """.formatted(name, city);
    }

    protected static String screenJson(String name) {
        return """
                {"name": "%s", "seatRows": 10, "seatsPerRow": 12}
                """.formatted(name);
    }

    protected static String showJson(UUID movieId, UUID screenId, String startsAt, String endsAt) {
        return """
                {"movieId": "%s", "screenId": "%s", "startsAt": "%s", "endsAt": "%s", "basePrice": 12.50}
                """.formatted(movieId, screenId, startsAt, endsAt);
    }
}
