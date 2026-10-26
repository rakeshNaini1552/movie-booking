package com.movie.booking.payment_service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestJwt {

    private TestJwt() {}

    // The real chain maps realm_access.roles to ROLE_<role>, so this is what an authenticated caller carries.
    public static RequestPostProcessor internalService() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_internal-service"));
    }

    public static RequestPostProcessor withoutInternalServiceRole() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_customer"));
    }
}
