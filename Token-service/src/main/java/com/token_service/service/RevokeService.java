package com.token_service.service;

import com.token_service.entity.RefreshTokenData;
import com.token_service.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevokeService {

    private static final String PREFIX = "refresh:";
    private static final String USER_INDEX = "user:refresh:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenHasher tokenHasher;

    public void revokeAllForUser(UUID userId) {

        String userKey = USER_INDEX + userId;

        Set<Object> tokens = redisTemplate.opsForSet().members(userKey);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        tokens.forEach(hash ->
                redisTemplate.delete(PREFIX + hash)
        );

        redisTemplate.delete(userKey);
    }

    public void revokeSingle(String rawToken, String deviceHash) {

        String hashed = tokenHasher.hash(rawToken);
        String key = PREFIX + hashed;

        RefreshTokenData data = (RefreshTokenData) redisTemplate.opsForValue().get(key);
        if (data == null) {
            return;
        }

        if (!data.getDeviceHash().equals(deviceHash)) {
            throw new RuntimeException("Device mismatch");
        }

        redisTemplate.delete(key);

        redisTemplate.opsForSet().remove(
                USER_INDEX + data.getUserId(),
                hashed
        );
    }
}
