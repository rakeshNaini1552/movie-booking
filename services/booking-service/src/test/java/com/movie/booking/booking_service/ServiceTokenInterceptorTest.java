package com.movie.booking.booking_service;

import com.movie.booking.booking_service.config.ServiceAuthConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServiceTokenInterceptorTest {

    private static final String BASE_URL = "http://seat-service";

    private final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);

    @Test
    void outgoing_call_carries_the_service_token_as_a_bearer_header() {
        when(manager.authorize(any())).thenReturn(authorizedClient("service-token-1"));
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .requestInterceptor(interceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/ping"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token-1"))
                .andRespond(withSuccess());

        builder.build().get().uri("/ping").retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    void token_is_requested_for_the_booking_service_registration_with_one_shared_principal() {
        when(manager.authorize(any())).thenReturn(authorizedClient("service-token-1"));
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .requestInterceptor(interceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/ping")).andRespond(withSuccess());
        server.expect(requestTo(BASE_URL + "/pong")).andRespond(withSuccess());
        RestClient client = builder.build();

        client.get().uri("/ping").retrieve().toBodilessEntity();
        client.get().uri("/pong").retrieve().toBodilessEntity();

        ArgumentCaptor<OAuth2AuthorizeRequest> captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(manager, times(2)).authorize(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(request -> {
            assertThat(request.getClientRegistrationId()).isEqualTo("booking-service");
            assertThat(request.getPrincipal().getName()).isEqualTo("booking-service");
        });
    }

    private ClientHttpRequestInterceptor interceptor() {
        return ServiceAuthConfig.bearerTokenInterceptor(manager);
    }

    private OAuth2AuthorizedClient authorizedClient(String tokenValue) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("booking-service")
                .clientId("booking-service")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:0/token")
                .build();
        Instant now = Instant.now();
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, tokenValue, now, now.plusSeconds(300));
        return new OAuth2AuthorizedClient(registration, "booking-service", token);
    }
}
