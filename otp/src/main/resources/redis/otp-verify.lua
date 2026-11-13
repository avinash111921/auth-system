-- Check a code and count the attempt in one step, so parallel guesses can't exceed the limit.
-- KEYS[1] = code hash key, KEYS[2] = attempts key; ARGV[1] = candidate HMAC, ARGV[2] = max attempts.
-- Returns 1 if the code matched (and consumes it), 0 if it didn't, -1 if there is no live code.
local stored = redis.call('GET', KEYS[1])
if not stored then
  return -1
end
if stored == ARGV[1] then
  redis.call('DEL', KEYS[1], KEYS[2])
  return 1
end
local attempts = redis.call('INCR', KEYS[2])
if attempts == 1 then
  redis.call('PEXPIRE', KEYS[2], redis.call('PTTL', KEYS[1]))
end
if attempts >= tonumber(ARGV[2]) then
  redis.call('DEL', KEYS[1], KEYS[2])
end
return 0
