package com.movie.booking.booking_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ServiceAuthConfig {

    static final String REGISTRATION_ID = "booking-service";
    private static final Duration REFRESH_BEFORE_EXPIRY = Duration.ofSeconds(60);

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    // The service-based manager does not need an HTTP request, so it also works on the outbox
    // publisher and Kafka listener threads.
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                                 OAuth2AuthorizedClientService clientService,
                                                                 Clock clock) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(c -> c.clockSkew(REFRESH_BEFORE_EXPIRY).clock(clock))
                .build());
        return manager;
    }

    @Bean
    public ClientHttpRequestInterceptor serviceTokenInterceptor(OAuth2AuthorizedClientManager manager) {
        return bearerTokenInterceptor(manager);
    }

    public static OAuth2ClientHttpRequestInterceptor bearerTokenInterceptor(OAuth2AuthorizedClientManager manager) {
        OAuth2ClientHttpRequestInterceptor interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(request -> REGISTRATION_ID);
        // One fixed principal so a single service token is cached, not one per calling user.
        interceptor.setPrincipalResolver(request -> new AnonymousAuthenticationToken(
                REGISTRATION_ID, REGISTRATION_ID, AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        return interceptor;
    }
}
