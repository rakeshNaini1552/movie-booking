package com.movie.booking.catalog_service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestJwt {

    private TestJwt() {}

    // Authenticated but without the admin role: may read, may not write.
    // Any authenticated user may call the catalog API for now; a dedicated admin role comes later.
    public static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_customer"));
    }

    // Same as the default of catalog.security.admin-role in application.yml.
    public static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_catalog-admin"));
    }
}
