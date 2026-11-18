package com.movie.booking.gateway_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.context.annotation.Import;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

// The defaults of the GATEWAY_*_TIMEOUT_MS settings, as bound by Spring.
@SpringBootTest
@Import(TestContainersConfig.class)
class GatewayTimeoutConfigTest {

	@Autowired
	HttpClientProperties httpClient;

	@Autowired
	DataRedisProperties redis;

	@Test
	void downstreamTimeoutsAreBound() {
		assertThat(httpClient.getConnectTimeout()).isEqualTo(2000);
		assertThat(httpClient.getResponseTimeout()).isEqualTo(Duration.ofSeconds(20));
	}

	@Test
	void redisCommandTimeoutIsOneSecond() {
		assertThat(redis.getTimeout()).isEqualTo(Duration.ofSeconds(1));
	}

}
