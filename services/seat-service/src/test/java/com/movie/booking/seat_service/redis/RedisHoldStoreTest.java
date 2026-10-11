package com.movie.booking.seat_service.redis;

import com.movie.booking.seat_service.TestContainersConfig;
import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.ExpiredHold;
import com.movie.booking.seat_service.service.HoldStore;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestContainersConfig.class)
class RedisHoldStoreTest {

    @Autowired
    HoldStore holdStore;
    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void failed_hold_leaves_no_seat_held() {
        UUID showId = UUID.randomUUID();
        holdStore.hold(showId, List.of("A2"), UUID.randomUUID(), Duration.ofMinutes(5)); // user Y holds A2

        assertThatThrownBy(() ->
                holdStore.hold(showId, List.of("A1", "A2", "A3"), UUID.randomUUID(), Duration.ofMinutes(5)))
                .isInstanceOf(SeatNotAvailableException.class);

        // the point of the test: A1 and A3 must NOT be held
        assertThat(redisTemplate.hasKey("hold:" + showId + ":A1")).isFalse();
        assertThat(redisTemplate.hasKey("hold:" + showId + ":A3")).isFalse();
    }

    @Test
    void release_frees_only_own_hold() {
        UUID showId = UUID.randomUUID();
        UUID bookingOfY = UUID.randomUUID();
        UUID bookingOfX = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), bookingOfY, Duration.ofMinutes(5));

        holdStore.release(showId, List.of("A1"), bookingOfX);          // X is not the owner
        assertThat(redisTemplate.hasKey(key)).isTrue();             // Y's hold is untouched

        holdStore.release(showId, List.of("A1"), bookingOfY);          // the owner releases
        assertThat(redisTemplate.hasKey(key)).isFalse();            // now it is free
    }

    @Test
    void is_held_by_is_false_after_expiry() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofSeconds(1));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), bookingId)).isTrue();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> !holdStore.isHeldBy(showId, List.of("A1"), bookingId));
    }

    @Test
    void extend_resets_ttl_when_owner(){

        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofSeconds(10));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), bookingId)).isTrue();

        boolean extended = holdStore.extend(showId, List.of("A1"), bookingId, Duration.ofSeconds(100));

        assertThat(extended).isTrue();
        assertThat(redisTemplate.getExpire(key)).isGreaterThan(10L);

    }

    @Test
    void extend_fails_when_not_owner(){

        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofSeconds(10));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), bookingId)).isTrue();

        boolean extended = holdStore.extend(showId, List.of("A1"), UUID.randomUUID(), Duration.ofSeconds(100));

        assertThat(extended).isFalse();
        assertThat(redisTemplate.getExpire(key)).isLessThanOrEqualTo(10L);

    }

    @Test
    void extend_fails_when_ttl_is_expired(){

        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofSeconds(2));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), bookingId)).isTrue();

        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .until(() -> !holdStore.isHeldBy(showId, List.of("A1"), bookingId));

        boolean extended = holdStore.extend(showId, List.of("A1"), bookingId, Duration.ofSeconds(100));

        assertThat(extended).isFalse();
        assertThat(redisTemplate.getExpire(key)).isEqualTo(-2L);

    }

    @Test
    void hold_with_same_booking_id_succeeds_again() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofSeconds(60));

        assertThatCode(() ->
                holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofSeconds(60)))
                .doesNotThrowAnyException();
    }

    @Test
    void hold_with_different_booking_id_is_rejected() {
        UUID showId = UUID.randomUUID();

        holdStore.hold(showId, List.of("A1"), UUID.randomUUID(), Duration.ofSeconds(60));

        assertThatThrownBy(() ->
                holdStore.hold(showId, List.of("A1"), UUID.randomUUID(), Duration.ofSeconds(60)))
                .isInstanceOf(SeatNotAvailableException.class);
    }

    private List<ExpiredHold> claimFor(UUID bookingId, Instant now) {
        return holdStore.claimExpired(now, 1000).stream()
                .filter(h -> h.bookingId().equals(bookingId))
                .toList();
    }

    @Test
    void claim_expired_returns_a_hold_only_once_it_is_due() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        holdStore.hold(showId, List.of("A2", "A1"), bookingId, Duration.ofMinutes(5));

        assertThat(claimFor(bookingId, Instant.now())).isEmpty();

        assertThat(claimFor(bookingId, Instant.now().plus(Duration.ofMinutes(6))))
                .containsExactly(new ExpiredHold(showId, List.of("A1", "A2"), bookingId));
    }

    @Test
    void claim_expired_hands_each_hold_to_one_caller() {
        UUID bookingId = UUID.randomUUID();
        holdStore.hold(UUID.randomUUID(), List.of("A1"), bookingId, Duration.ofMinutes(5));
        Instant later = Instant.now().plus(Duration.ofMinutes(6));

        assertThat(claimFor(bookingId, later)).hasSize(1);
        assertThat(claimFor(bookingId, later)).isEmpty();
    }

    @Test
    void retrying_the_hold_does_not_create_a_second_expiry_entry() {
        UUID bookingId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofMinutes(5));
        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofMinutes(5));

        assertThat(claimFor(bookingId, Instant.now().plus(Duration.ofMinutes(6)))).hasSize(1);
    }

    @Test
    void release_removes_the_hold_from_expiry_tracking() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofMinutes(5));

        holdStore.release(showId, List.of("A1"), bookingId);

        assertThat(claimFor(bookingId, Instant.now().plus(Duration.ofMinutes(6)))).isEmpty();
    }

    @Test
    void extend_pushes_the_expiry_time_out() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        holdStore.hold(showId, List.of("A1"), bookingId, Duration.ofMinutes(5));

        holdStore.extend(showId, List.of("A1"), bookingId, Duration.ofMinutes(30));

        assertThat(claimFor(bookingId, Instant.now().plus(Duration.ofMinutes(6)))).isEmpty();
        assertThat(claimFor(bookingId, Instant.now().plus(Duration.ofMinutes(31)))).hasSize(1);
    }
}
