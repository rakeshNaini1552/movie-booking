package com.movie.booking.gateway_service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redis() {
		return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
	}

}
