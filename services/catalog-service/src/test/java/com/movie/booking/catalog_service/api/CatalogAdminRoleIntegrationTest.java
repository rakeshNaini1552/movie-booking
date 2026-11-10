package com.movie.booking.catalog_service.api;

import com.movie.booking.catalog_service.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogAdminRoleIntegrationTest extends ApiTestBase {

    @Test
    void admin_can_create_and_update() throws Exception {
        UUID movie = createMovie("Dune");
        UUID theater = createTheater("PVR", "Pune");
        UUID screen = createScreen(theater, "Screen 1");
        UUID show = createAndGetId("/api/v1/shows",
                showJson(movie, screen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));

        mockMvc.perform(put("/api/v1/movies/{id}", movie).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON).content(movieJson("Dune 2")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Dune 2"));
        mockMvc.perform(put("/api/v1/theaters/{id}", theater).with(TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON).content(theaterJson("PVR 2", "Pune")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", show).with(TestJwt.admin()))
                .andExpect(status().isOk());
    }

    @Test
    void a_token_without_the_role_gets_403_on_every_write_method_and_nothing_changes() throws Exception {
        UUID movie = createMovie("Dune");
        UUID theater = createTheater("PVR", "Pune");
        UUID screen = createScreen(theater, "Screen 1");
        UUID show = createAndGetId("/api/v1/shows",
                showJson(movie, screen, "2030-02-01T18:00:00Z", "2030-02-01T20:00:00Z"));
        RequestPostProcessor user = TestJwt.user();
        String json = MediaType.APPLICATION_JSON_VALUE;

        mockMvc.perform(post("/api/v1/movies").with(user).contentType(json).content(movieJson("X"))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/movies/{id}", movie).with(user).contentType(json).content(movieJson("X"))).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/movies/{id}", movie).with(user).contentType(json).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/movies/{id}", movie).with(user)).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/theaters").with(user).contentType(json).content(theaterJson("X", "Y"))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/theaters/{id}", theater).with(user).contentType(json).content(theaterJson("X", "Y"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/theaters/{id}", theater).with(user)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/theaters/{id}/screens", theater).with(user).contentType(json).content(screenJson("S2"))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/theaters/{t}/screens/{s}", theater, screen).with(user).contentType(json).content(screenJson("S2"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/theaters/{t}/screens/{s}", theater, screen).with(user)).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/shows").with(user).contentType(json)
                .content(showJson(movie, screen, "2030-03-01T18:00:00Z", "2030-03-01T20:00:00Z"))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/shows/{id}", show).with(user).contentType(json)
                .content(showJson(movie, screen, "2030-03-01T18:00:00Z", "2030-03-01T20:00:00Z"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/shows/{id}/cancel", show).with(user)).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/movies/{id}", movie).with(TestJwt.admin()))
                .andExpect(jsonPath("$.title").value("Dune"));
        mockMvc.perform(get("/api/v1/shows/{id}", show).with(TestJwt.admin()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void reads_and_graphql_work_with_or_without_the_role() throws Exception {
        UUID movie = createMovie("Dune");
        for (RequestPostProcessor token : new RequestPostProcessor[]{TestJwt.user(), TestJwt.admin()}) {
            mockMvc.perform(get("/api/v1/movies/{id}", movie).with(token)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/movies").with(token)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/theaters").with(token)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/shows").with(token)).andExpect(status().isOk());
        }
        graphQl("{\"query\": \"{ movies { title } }\"}")
                .andExpect(jsonPath("$.data.movies[0].title").value("Dune"));
    }
}
