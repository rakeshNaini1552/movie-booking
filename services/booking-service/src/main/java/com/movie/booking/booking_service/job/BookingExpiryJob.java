package com.movie.booking.booking_service.job;

import com.movie.booking.booking_service.service.BookingExpiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.expiry.enabled", havingValue = "true")
public class BookingExpiryJob {

    private final BookingExpiryService bookingExpiryService;

    @Scheduled(fixedDelayString = "${booking.expiry.poll-interval-ms}")
    public void expireStaleBookings() {
        bookingExpiryService.expireStaleBookings();
    }
}
