-- Give one attempt back, never going below zero. KEYS[1] = counter.
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
if current > 0 then
  return redis.call('DECR', KEYS[1])
end
return 0
