-- Atomically consume a refresh token and leave a "used" marker naming its family.
-- KEYS[1] = token key, KEYS[2] = used-marker key; ARGV[1] = marker TTL in seconds.
-- Returns the stored session, or nil if the token was already consumed, revoked or expired.
local session = redis.call('GETDEL', KEYS[1])
if session then
  local family = string.match(session, '^[^|]*|([^|]*)|')
  redis.call('SET', KEYS[2], family, 'EX', ARGV[1])
end
return session
