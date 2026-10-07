package com.movie.booking.seat_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class Config {

    @Bean
    Clock clock(){
        return Clock.systemUTC();
    }

    @Bean
    Duration holdTtl(@Value("${seat.hold.ttl}") Duration ttl) {
        return ttl;
    }
}
