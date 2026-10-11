package com.movie.booking.seat_service.job;

import com.movie.booking.seat_service.service.HoldExpiryService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs on every pod; ShedLock lets only one pod through per tick. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "seat.hold.expiry.enabled", havingValue = "true")
public class HoldExpiryJob {

    public static final String LOCK_NAME = "holdExpiryJob";

    private final HoldExpiryService holdExpiryService;

    @Scheduled(fixedDelayString = "${seat.hold.expiry.poll-interval}")
    @SchedulerLock(name = LOCK_NAME,
            lockAtMostFor = "${seat.hold.expiry.lock-at-most-for}",
            lockAtLeastFor = "${seat.hold.expiry.lock-at-least-for}")
    public void releaseExpiredHolds() {
        holdExpiryService.releaseExpiredHolds();
    }
}
