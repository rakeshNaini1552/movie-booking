package com.movie.booking.booking_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock(){
        return Clock.systemUTC();
    }

    @Bean
    public RestClient seatServiceRestClient(
            @Value("${seat-service.base-url}") String baseUrl,
            @Value("${seat-service.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${seat-service.read-timeout-ms}") long readTimeoutMs,
            ClientHttpRequestInterceptor serviceTokenInterceptor,
            ClientHttpRequestInterceptor requestIdInterceptor) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(serviceTokenInterceptor)
                .requestInterceptor(requestIdInterceptor)
                .build();
    }

    @Bean
    public RestClient paymentServiceRestClient(
            @Value("${payment-service.base-url}") String baseUrl,
            @Value("${payment-service.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${payment-service.read-timeout-ms}") long readTimeoutMs,
            ClientHttpRequestInterceptor serviceTokenInterceptor,
            ClientHttpRequestInterceptor requestIdInterceptor) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(serviceTokenInterceptor)
                .requestInterceptor(requestIdInterceptor)
                .build();
    }
}
