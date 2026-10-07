-- KEYS = one key per seat, ARGV[1] = holdId, ARGV[2] = ttl in seconds
for i = 1, #KEYS do
	if redis.call('EXISTS', KEYS[i]) == 1 then
		return 0                      -- some seat is taken → change nothing
	end
end
for i = 1, #KEYS do
	redis.call('SET', KEYS[i], ARGV[1], 'EX', ARGV[2])
end
return 1                          -- all seats held