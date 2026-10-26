package com.movie.booking.payment_service.service;

import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.entity.ProcessedEvent;
import com.movie.booking.payment_service.event.RefundRequestedEvent;
import com.movie.booking.payment_service.repository.PaymentRepository;
import com.movie.booking.payment_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final Clock clock;

    @Transactional
    public void refund(RefundRequestedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Event {} already processed, skipping refund for booking {}", event.eventId(), event.bookingId());
            return;
        }
        Instant now = clock.instant();
        processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId(), now));

        int refunded = paymentRepository.transitionByBookingId(
                event.bookingId(), PaymentStatus.SUCCESS, PaymentStatus.REFUNDED, now);
        if (refunded == 0) {
            log.warn("No SUCCESS payment for booking {}, nothing to refund (event {})",
                    event.bookingId(), event.eventId());
            return;
        }
        log.info("Refunded payment for booking {} (event {})", event.bookingId(), event.eventId());
    }
}
