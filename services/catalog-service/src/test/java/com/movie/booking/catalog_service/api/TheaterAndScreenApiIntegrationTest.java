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

class TheaterAndScreenApiIntegrationTest extends ApiTestBase {

    @Test
    void theater_create_get_update_delete_round_trip() throws Exception {
        UUID id = createTheater("PVR", "Pune");

        mockMvc.perform(get("/api/v1/theaters/{id}", id).with(TestJwt.user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PVR"))
                .andExpect(jsonPath("$.city").value("Pune"));
        mockMvc.perform(put("/api/v1/theaters/{id}", id).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON).content(theaterJson("INOX", "Mumbai")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("INOX"));
        mockMvc.perform(delete("/api/v1/theaters/{id}", id).with(TestJwt.admin())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/theaters/{id}", id).with(TestJwt.user())).andExpect(status().isNotFound());
    }

    @Test
    void theater_list_filters_by_city_and_is_paged() throws Exception {
        createTheater("PVR 1", "Pune");
        createTheater("PVR 2", "Pune");
        createTheater("INOX", "Mumbai");

        mockMvc.perform(get("/api/v1/theaters").param("city", "pune").with(TestJwt.user()))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/theaters").param("size", "1").with(TestJwt.user()))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void theater_validation_error_returns_400() throws Exception {
        postJson("/api/v1/theaters", "{\"name\": \" \", \"city\": \"Pune\", \"address\": \"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void deleting_a_theater_also_removes_its_screens() throws Exception {
        UUID theaterId = createTheater("PVR", "Pune");
        createScreen(theaterId, "Screen 1");

        mockMvc.perform(delete("/api/v1/theaters/{id}", theaterId).with(TestJwt.admin()))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM screen", Integer.class)).isZero();
    }

    @Test
    void deleting_a_theater_whose_screen_has_shows_returns_409_and_keeps_everything() throws Exception {
        UUID theaterId = createTheater("PVR", "Pune");
        UUID screenId = createScreen(theaterId, "Screen 1");
        createAndGetId("/api/v1/shows",
                showJson(createMovie("Dune"), screenId, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));

        mockMvc.perform(delete("/api/v1/theaters/{id}", theaterId).with(TestJwt.admin()))
                .andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM theater", Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM screen", Integer.class)).isOne();
    }

    @Test
    void screen_crud_under_a_theater() throws Exception {
        UUID theaterId = createTheater("PVR", "Pune");
        UUID screenId = createScreen(theaterId, "Screen 1");

        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theaterId, screenId).with(TestJwt.user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theaterId").value(theaterId.toString()))
                .andExpect(jsonPath("$.seatRows").value(10))
                .andExpect(jsonPath("$.seatsPerRow").value(12));
        mockMvc.perform(get("/api/v1/theaters/{t}/screens", theaterId).with(TestJwt.user()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(put("/api/v1/theaters/{t}/screens/{s}", theaterId, screenId).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"IMAX\", \"seatRows\": 20, \"seatsPerRow\": 30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IMAX"))
                .andExpect(jsonPath("$.seatRows").value(20));
        mockMvc.perform(delete("/api/v1/theaters/{t}/screens/{s}", theaterId, screenId).with(TestJwt.admin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", theaterId, screenId).with(TestJwt.user()))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicate_screen_name_in_the_same_theater_returns_409_but_is_fine_in_another_theater() throws Exception {
        UUID theaterId = createTheater("PVR", "Pune");
        createScreen(theaterId, "Screen 1");

        postJson("/api/v1/theaters/{id}/screens", screenJson("Screen 1"), theaterId)
                .andExpect(status().isConflict());
        createScreen(createTheater("INOX", "Mumbai"), "Screen 1");
    }

    @Test
    void screen_under_an_unknown_theater_returns_404() throws Exception {
        postJson("/api/v1/theaters/{id}/screens", screenJson("Screen 1"), UUID.randomUUID())
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/theaters/{id}/screens", UUID.randomUUID()).with(TestJwt.user()))
                .andExpect(status().isNotFound());
    }

    @Test
    void screen_addressed_through_the_wrong_theater_returns_404() throws Exception {
        UUID screenId = createScreen(createTheater("PVR", "Pune"), "Screen 1");
        UUID otherTheater = createTheater("INOX", "Mumbai");

        mockMvc.perform(get("/api/v1/theaters/{t}/screens/{s}", otherTheater, screenId).with(TestJwt.user()))
                .andExpect(status().isNotFound());
    }

    @Test
    void screen_dimensions_beyond_the_seat_id_format_return_400() throws Exception {
        UUID theaterId = createTheater("PVR", "Pune");

        postJson("/api/v1/theaters/{id}/screens",
                "{\"name\": \"Big\", \"seatRows\": 27, \"seatsPerRow\": 10}", theaterId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("seatRows"));
    }

    @Test
    void deleting_a_screen_with_shows_returns_409() throws Exception {
        UUID theaterId = createTheater("PVR", "Pune");
        UUID screenId = createScreen(theaterId, "Screen 1");
        createAndGetId("/api/v1/shows",
                showJson(createMovie("Dune"), screenId, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));

        mockMvc.perform(delete("/api/v1/theaters/{t}/screens/{s}", theaterId, screenId).with(TestJwt.admin()))
                .andExpect(status().isConflict());
    }
}
