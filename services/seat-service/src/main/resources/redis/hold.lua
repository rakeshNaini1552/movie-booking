-- KEYS = one key per seat, ARGV[1] = bookingId (the hold id), ARGV[2] = ttl in seconds
-- ARGV[3] = expiry sorted set, ARGV[4] = member describing this hold, ARGV[5] = expiry time (epoch millis)
for i = 1, #KEYS do
	local current = redis.call('GET', KEYS[i])
	if current ~= false and current ~= ARGV[1] then
		return 0                      -- held by a different booking → change nothing
	end
end
for i = 1, #KEYS do
	redis.call('SET', KEYS[i], ARGV[1], 'EX', ARGV[2])
end
redis.call('ZADD', ARGV[3], ARGV[5], ARGV[4])   -- a retry just moves the same member's score
return 1                              -- all seats held (new hold or a retry)
