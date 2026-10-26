package com.movie.booking.booking_service;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

final class TestAuth {

    private TestAuth() {}

    static RequestPostProcessor asUser(UUID userId) {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }
}
