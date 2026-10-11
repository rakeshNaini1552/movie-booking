-- KEYS = seat keys, ARGV[1] = bookingId
-- ARGV[2] = expiry sorted set, ARGV[3] = member describing this hold
for i = 1, #KEYS do
	if redis.call('GET', KEYS[i]) == ARGV[1] then   -- is this seat held by the caller?
		redis.call('DEL', KEYS[i])                    -- yes → free it
	end                                             -- no (expired or someone else's) → leave it alone
end
redis.call('ZREM', ARGV[2], ARGV[3])                -- nothing left to expire for this hold
return 1
