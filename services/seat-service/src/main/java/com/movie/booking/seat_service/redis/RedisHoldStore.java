package com.movie.booking.seat_service.redis;

import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.HoldStore;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisHoldStore implements HoldStore {

    private final StringRedisTemplate redis;
    private static final RedisScript<Long> HOLD_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/hold.lua"), Long.class);

    private static final RedisScript<Long> RELEASE_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/release.lua"), Long.class);

    private String key(UUID showId, String seatId) {
        return "hold:" + showId + ":" + seatId;
    }

    @Override
    public void hold(UUID showId, List<String> seatIds, UUID holdId, Duration ttl) {
        List<String> keys = seatIds.stream().map(id -> key(showId, id)).toList();

        Long result = redis.execute(HOLD_SCRIPT, keys, holdId.toString(), String.valueOf(ttl.toSeconds()));

        if (result == null || result == 0L) {
            throw new SeatNotAvailableException(showId, seatIds);
        }
    }

    @Override
    public boolean isHeldBy(UUID showId, List<String> seatIds, UUID holdId) {
        List<String> keys = seatIds.stream().map(id -> key(showId, id)).toList();
        List<String> values = redis.opsForValue().multiGet(keys);   // one round trip, values in the same order
        // true only if every value equals holdId.toString()
        return values != null && values.stream().allMatch(holdId.toString()::equals);
    }


    @Override
    public void release(UUID showId, List<String> seatIds, UUID holdId) {
        List<String> keys = seatIds.stream().map(id -> key(showId, id)).toList();
        redis.execute(RELEASE_SCRIPT, keys, holdId.toString());
    }
}
