package com.movie.booking.seat_service.redis;

import com.movie.booking.seat_service.TestContainersConfig;
import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.HoldStore;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;


import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        UUID holdOfY = UUID.randomUUID();
        UUID holdOfX = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), holdOfY, Duration.ofMinutes(5));

        holdStore.release(showId, List.of("A1"), holdOfX);          // X is not the owner
        assertThat(redisTemplate.hasKey(key)).isTrue();             // Y's hold is untouched

        holdStore.release(showId, List.of("A1"), holdOfY);          // the owner releases
        assertThat(redisTemplate.hasKey(key)).isFalse();            // now it is free
    }

    @Test
    void is_held_by_is_false_after_expiry() {
        UUID showId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();

        holdStore.hold(showId, List.of("A1"), holdId, Duration.ofSeconds(1));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), holdId)).isTrue();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> !holdStore.isHeldBy(showId, List.of("A1"), holdId));
    }

    @Test
    void extend_resets_ttl_when_owner(){

        UUID showId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), holdId, Duration.ofSeconds(10));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), holdId)).isTrue();

        boolean extended = holdStore.extend(showId, List.of("A1"), holdId, Duration.ofSeconds(100));

        assertThat(extended).isTrue();
        assertThat(redisTemplate.getExpire(key)).isGreaterThan(10L);

    }

    @Test
    void extend_fails_when_not_owner(){

        UUID showId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), holdId, Duration.ofSeconds(10));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), holdId)).isTrue();

        boolean extended = holdStore.extend(showId, List.of("A1"), UUID.randomUUID(), Duration.ofSeconds(100));

        assertThat(extended).isFalse();
        assertThat(redisTemplate.getExpire(key)).isLessThanOrEqualTo(10L);

    }

    @Test
    void extend_fails_when_ttl_is_expired(){

        UUID showId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        String key = "hold:" + showId + ":A1";

        holdStore.hold(showId, List.of("A1"), holdId, Duration.ofSeconds(2));
        assertThat(holdStore.isHeldBy(showId, List.of("A1"), holdId)).isTrue();

        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .until(() -> !holdStore.isHeldBy(showId, List.of("A1"), holdId));

        boolean extended = holdStore.extend(showId, List.of("A1"), holdId, Duration.ofSeconds(100));

        assertThat(extended).isFalse();
        assertThat(redisTemplate.getExpire(key)).isEqualTo(-2L);

    }
}
