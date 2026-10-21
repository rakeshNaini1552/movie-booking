package com.movie.booking.ticketservice.config;

import com.movie.booking.ticketservice.consumer.TicketIssueFailedRecoverer;
import com.movie.booking.ticketservice.service.TicketFailureService;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            TicketFailureService ticketFailureService,
            JsonMapper jsonMapper,
            @Value("${ticket.consumer.dlt-topic}") String dltTopic,
            @Value("${ticket.consumer.retry.max-retries}") int maxRetries,
            @Value("${ticket.consumer.retry.initial-interval-ms}") long initialIntervalMs,
            @Value("${ticket.consumer.retry.multiplier}") double multiplier,
            @Value("${ticket.consumer.retry.max-interval-ms}") long maxIntervalMs) {

        DeadLetterPublishingRecoverer deadLetterRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(dltTopic, -1));

        TicketIssueFailedRecoverer recoverer =
                new TicketIssueFailedRecoverer(deadLetterRecoverer, ticketFailureService, jsonMapper);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxIntervalMs);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(JacksonException.class);
        return errorHandler;
    }
}