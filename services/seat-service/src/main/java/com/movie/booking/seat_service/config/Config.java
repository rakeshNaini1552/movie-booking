package com.movie.booking.seat_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class Config {

    @Bean
    Clock clock(){
        return Clock.systemUTC();
    }
}
