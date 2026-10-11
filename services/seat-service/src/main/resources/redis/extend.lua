-- KEYS = seat keys, ARGV[1] = bookingId, ARGV[2] = new TTL in seconds
-- ARGV[3] = expiry sorted set, ARGV[4] = member describing this hold, ARGV[5] = new expiry time (epoch millis)
for i = 1, #KEYS do
	if redis.call('GET', KEYS[i]) ~= ARGV[1] then
		return 0          -- not ours (expired or someone else's): change nothing
	end
end
for i = 1, #KEYS do
	redis.call('EXPIRE', KEYS[i], ARGV[2])
end
redis.call('ZADD', ARGV[3], ARGV[5], ARGV[4])
return 1
