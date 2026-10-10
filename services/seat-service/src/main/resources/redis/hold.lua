-- KEYS = one key per seat, ARGV[1] = bookingId (the hold id), ARGV[2] = ttl in seconds
for i = 1, #KEYS do
	local current = redis.call('GET', KEYS[i])
	if current ~= false and current ~= ARGV[1] then
		return 0                      -- held by a different booking → change nothing
	end
end
for i = 1, #KEYS do
	redis.call('SET', KEYS[i], ARGV[1], 'EX', ARGV[2])
end
return 1                              -- all seats held (new hold or a retry)