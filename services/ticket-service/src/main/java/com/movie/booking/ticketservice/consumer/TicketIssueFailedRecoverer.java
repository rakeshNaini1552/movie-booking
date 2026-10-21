package com.movie.booking.ticketservice.consumer;

import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.service.TicketFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@RequiredArgsConstructor
public class TicketIssueFailedRecoverer implements ConsumerRecordRecoverer {

    private static final int MAX_MESSAGE_LENGTH = 200;

    private final ConsumerRecordRecoverer deadLetterRecoverer;
    private final TicketFailureService ticketFailureService;
    private final JsonMapper jsonMapper;

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        reportFailure(record, exception);
        deadLetterRecoverer.accept(record, exception);
    }

    private void reportFailure(ConsumerRecord<?, ?> record, Exception exception) {
        BookingConfirmedEvent event = parse(record);
        if (event == null || event.bookingId() == null || event.userId() == null) {
            log.warn("Dead-lettering record {}-{}@{} without TicketIssueFailed: no bookingId/userId in payload",
                    record.topic(), record.partition(), record.offset());
            return;
        }
        ticketFailureService.reportIssueFailed(event.bookingId(), event.userId(), reasonFor(exception));
        log.warn("Reported TicketIssueFailed for booking {}", event.bookingId());
    }

    private BookingConfirmedEvent parse(ConsumerRecord<?, ?> record) {
        if (!(record.value() instanceof String payload)) {
            return null;
        }
        try {
            return jsonMapper.readValue(payload, BookingConfirmedEvent.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String reasonFor(Exception exception) {
        Throwable cause = exception;
        if (exception instanceof ListenerExecutionFailedException && exception.getCause() != null) {
            cause = exception.getCause();
        }
        String message = cause.getMessage();
        if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }
        return cause.getClass().getName() + ": " + message;
    }
}
