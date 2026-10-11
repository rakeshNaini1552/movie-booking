-- KEYS[1] = expiry sorted set, ARGV[1] = now (epoch millis), ARGV[2] = max holds to claim
-- Removes and returns the due members, so one caller owns each expired hold.
local due = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2])
for i = 1, #due do
	redis.call('ZREM', KEYS[1], due[i])
end
return due
