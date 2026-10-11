package com.movie.booking.seat_service.redis;

import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.HoldStore;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisHoldStore implements HoldStore {

    private final StringRedisTemplate redis;
    private static final RedisScript<Long> HOLD_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/hold.lua"), Long.class);

    private static final RedisScript<Long> RELEASE_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/release.lua"), Long.class);

    private static final RedisScript<Long> EXTEND_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/extend.lua"), Long.class);

    private String key(UUID showId, String seatId) {
        return "hold:" + showId + ":" + seatId;
    }

    @Override
    public void hold(UUID showId, List<String> seatIds, UUID bookingId, Duration ttl) {
        List<String> keys = getKeys(showId, seatIds);

        Long result = redis.execute(HOLD_SCRIPT, keys, bookingId.toString(), String.valueOf(ttl.toSeconds()));

        if (result == null || result == 0L) {
            throw new SeatNotAvailableException(showId, seatIds);
        }
    }

    @Override
    public boolean isHeldBy(UUID showId, List<String> seatIds, UUID bookingId) {
        List<String> keys = getKeys(showId, seatIds);
        List<String> values = redis.opsForValue().multiGet(keys);   // one round trip, values in the same order
        // true only if every value equals bookingId.toString()
        return values != null && values.stream().allMatch(bookingId.toString()::equals);
    }


    @Override
    public void release(UUID showId, List<String> seatIds, UUID bookingId) {
        List<String> keys = getKeys(showId, seatIds);
        redis.execute(RELEASE_SCRIPT, keys, bookingId.toString());
    }

    @Override
    public boolean extend(UUID showId, List<String> seatIds, UUID bookingId, Duration ttl) {
        List<String> keys = getKeys(showId, seatIds);
        Long result
                = redis.execute(EXTEND_SCRIPT, keys, bookingId.toString(), String.valueOf(ttl.toSeconds()));
        return Long.valueOf(1).equals(result);
    }

    @Override
    public List<String> getHeldSeats(UUID showId, List<String> seatIds) {
        List<String> keys = getKeys(showId, seatIds);
        List<String> values = redis.opsForValue().multiGet(keys);   // one round trip, values in the same order
        // true only if every value equals bookingId.toString()
        List<String> held = new ArrayList<>();
        for (int i = 0; i < seatIds.size(); i++) {
            if (values.get(i) !=null ) {      // what check means "someone holds this seat"?
                held.add(seatIds.get(i));
            }
        }
        return held;
    }

    private @NonNull List<String> getKeys(UUID showId, List<String> seatIds) {
        return seatIds.stream().map(seatId -> key(showId, seatId)).toList();
    }
}
