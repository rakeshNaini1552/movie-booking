package com.movie.booking.catalog_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void api_docs_are_open_and_declare_the_bearer_jwt_scheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("catalog-service"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearer-jwt").exists());
    }

    @Test
    void api_routes_still_require_a_token() throws Exception {
        mockMvc.perform(get("/api/v1/movies")).andExpect(status().isUnauthorized());
    }

    @Test
    void a_read_documents_401_and_a_write_documents_401_and_403() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/movies/{id}'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/movies/{id}'].get.responses['403']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/movies'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/movies'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/movies/{id}'].delete.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/shows/{id}/cancel'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/theaters/{theaterId}/screens'].get.responses['401']").exists());
    }
}
