package com.movie.booking.seat_service.redis;

import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.ExpiredHold;
import com.movie.booking.seat_service.service.HoldStore;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisHoldStore implements HoldStore {

    private static final String EXPIRY_KEY = "hold:expiry";
    private static final String MEMBER_SEPARATOR = "|";
    private static final String SEAT_SEPARATOR = ",";

    private final StringRedisTemplate redis;
    private final Clock clock;
    private static final RedisScript<Long> HOLD_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/hold.lua"), Long.class);

    private static final RedisScript<Long> RELEASE_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/release.lua"), Long.class);

    private static final RedisScript<Long> EXTEND_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/extend.lua"), Long.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final RedisScript<List<String>> CLAIM_EXPIRED_SCRIPT =
            (RedisScript) RedisScript.of(new ClassPathResource("redis/claim_expired.lua"), List.class);

    private String key(UUID showId, String seatId) {
        return "hold:" + showId + ":" + seatId;
    }

    @Override
    public void hold(UUID showId, List<String> seatIds, UUID bookingId, Duration ttl) {
        List<String> keys = getKeys(showId, seatIds);

        Long result = redis.execute(HOLD_SCRIPT, keys, bookingId.toString(), String.valueOf(ttl.toSeconds()),
                EXPIRY_KEY, member(showId, seatIds, bookingId), expiresAt(ttl));

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
        redis.execute(RELEASE_SCRIPT, keys, bookingId.toString(), EXPIRY_KEY, member(showId, seatIds, bookingId));
    }

    @Override
    public boolean extend(UUID showId, List<String> seatIds, UUID bookingId, Duration ttl) {
        List<String> keys = getKeys(showId, seatIds);
        Long result
                = redis.execute(EXTEND_SCRIPT, keys, bookingId.toString(), String.valueOf(ttl.toSeconds()),
                        EXPIRY_KEY, member(showId, seatIds, bookingId), expiresAt(ttl));
        return Long.valueOf(1).equals(result);
    }

    @Override
    public List<ExpiredHold> claimExpired(Instant now, int limit) {
        List<String> members = redis.execute(CLAIM_EXPIRED_SCRIPT, List.of(EXPIRY_KEY),
                String.valueOf(now.toEpochMilli()), String.valueOf(limit));
        if (members == null) {
            return Collections.emptyList();
        }
        return members.stream().map(this::parseMember).toList();
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

    /** One sorted-set member per hold: showId|bookingId|seat,seat (seats sorted so retries match). */
    private String member(UUID showId, List<String> seatIds, UUID bookingId) {
        String seats = seatIds.stream().sorted().collect(Collectors.joining(SEAT_SEPARATOR));
        return showId + MEMBER_SEPARATOR + bookingId + MEMBER_SEPARATOR + seats;
    }

    private ExpiredHold parseMember(String member) {
        String[] parts = member.split("\\" + MEMBER_SEPARATOR, 3);
        return new ExpiredHold(UUID.fromString(parts[0]), List.of(parts[2].split(SEAT_SEPARATOR)), UUID.fromString(parts[1]));
    }

    private String expiresAt(Duration ttl) {
        return String.valueOf(clock.instant().plus(ttl).toEpochMilli());
    }

    private @NonNull List<String> getKeys(UUID showId, List<String> seatIds) {
        return seatIds.stream().map(seatId -> key(showId, seatId)).toList();
    }
}
