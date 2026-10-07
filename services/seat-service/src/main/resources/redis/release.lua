-- KEYS = seat keys, ARGV[1] = holdId
for i = 1, #KEYS do
	if redis.call('GET', KEYS[i]) == ARGV[1] then   -- is this seat held by the caller?
		redis.call('DEL', KEYS[i])                    -- yes → free it
	end                                             -- no (expired or someone else's) → leave it alone
end
return 1