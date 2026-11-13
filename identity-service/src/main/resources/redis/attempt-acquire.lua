-- Count one attempt against a limit in a single step, so parallel requests can't overshoot it.
-- KEYS[1] = counter; ARGV[1] = limit; ARGV[2] = window in milliseconds.
-- Returns the new count, or -1 (and counts nothing) if the limit was already reached.
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
if current >= tonumber(ARGV[1]) then
  return -1
end
local count = redis.call('INCR', KEYS[1])
-- Also repairs a counter that lost its TTL, so no key can lock anyone out forever.
if redis.call('PTTL', KEYS[1]) < 0 then
  redis.call('PEXPIRE', KEYS[1], ARGV[2])
end
return count
