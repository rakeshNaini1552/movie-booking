package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.TestJwt;
import com.movie.booking.catalog_service.advice.CatalogApiExceptionHandler;
import com.movie.booking.catalog_service.config.SecurityConfig;
import com.movie.booking.catalog_service.exception.ConflictException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import com.movie.booking.catalog_service.service.MovieService;
import com.movie.booking.catalog_service.service.ScreenService;
import com.movie.booking.catalog_service.service.ShowService;
import com.movie.booking.catalog_service.service.TheaterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MovieController.class, TheaterController.class, ScreenController.class, ShowController.class})
@Import({SecurityConfig.class, CatalogApiExceptionHandler.class})
class CatalogControllerSliceTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MovieService movieService;
    @MockitoBean private TheaterService theaterService;
    @MockitoBean private ScreenService screenService;
    @MockitoBean private ShowService showService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static final String PROBLEM_JSON = "application/problem+json";

    @Test
    void movie_with_invalid_fields_returns_400_listing_every_field_error() throws Exception {
        mockMvc.perform(post("/api/v1/movies").with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  ", "durationMinutes": 0, "language": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field=='title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='durationMinutes')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='language')]").exists());
    }

    @Test
    void movie_without_duration_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/movies").with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Dune\", \"language\": \"en\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='durationMinutes')]").exists());
    }

    @Test
    void theater_with_blank_name_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/theaters").with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"city\": \"Pune\", \"address\": \"MG Road\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void screen_with_non_positive_or_too_large_dimensions_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/theaters/{id}/screens", UUID.randomUUID()).with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"S1\", \"seatRows\": 0, \"seatsPerRow\": 100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='seatRows')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='seatsPerRow')]").exists());
    }

    @Test
    void show_with_end_before_start_and_negative_price_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/shows").with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movieId": "%s", "screenId": "%s",
                                 "startsAt": "2030-02-01T20:00:00Z", "endsAt": "2030-02-01T18:00:00Z",
                                 "basePrice": -1}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='endsAfterStart')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='basePrice')]").exists());
    }

    @Test
    void malformed_json_returns_400_problem_detail() throws Exception {
        mockMvc.perform(post("/api/v1/movies").with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad request"));
    }

    @Test
    void malformed_path_id_returns_400() throws Exception {
        mockMvc.perform(get("/api/v1/movies/not-a-uuid").with(TestJwt.user()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void not_found_returns_404_problem_detail() throws Exception {
        UUID id = UUID.randomUUID();
        when(movieService.get(id)).thenThrow(new ResourceNotFoundException("Movie", id));

        mockMvc.perform(get("/api/v1/movies/{id}", id).with(TestJwt.user()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Movie not found"))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void conflict_returns_409_problem_detail() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ConflictException("Theater has shows")).when(theaterService).delete(id);

        mockMvc.perform(delete("/api/v1/theaters/{id}", id).with(TestJwt.user()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Theater has shows"));
    }

    @Test
    void unexpected_error_returns_500_without_leaking_details() throws Exception {
        when(showService.cancel(any())).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(post("/api/v1/shows/{id}/cancel", UUID.randomUUID()).with(TestJwt.user()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret internal detail"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unsupported_method_stays_405_not_500() throws Exception {
        mockMvc.perform(put("/api/v1/shows").with(TestJwt.user())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void requests_without_a_token_return_401() throws Exception {
        mockMvc.perform(get("/api/v1/movies")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }
}
