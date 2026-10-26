package com.movie.booking.booking_service.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaHealthIndicator implements HealthIndicator, DisposableBean {

    private final AdminClient adminClient;
    private final int timeoutMs;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin,
                                @Value("${booking.health.kafka-timeout-ms}") int timeoutMs) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Health health() {
        try {
            String clusterId = adminClient
                    .describeCluster(new DescribeClusterOptions().timeoutMs(timeoutMs))
                    .clusterId()
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            return Health.up().withDetail("clusterId", clusterId).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.down(e).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }

    @Override
    public void destroy() {
        adminClient.close();
    }
}