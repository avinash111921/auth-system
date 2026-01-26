package com.token_service.service;

import com.token_service.config.JwtProperties;
import com.token_service.entity.RefreshTokenData;
import com.token_service.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String PREFIX = "refresh:";
    private static final String USER_INDEX = "user:refresh:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenHasher hasher;
    private final JwtProperties props;

    public String create(UUID userId, String deviceHash, String parentJti) {

        String rawToken = UUID.randomUUID().toString();
        String hashed = hasher.hash(rawToken);

        RefreshTokenData data =
                new RefreshTokenData(userId, deviceHash, parentJti);

        redisTemplate.opsForValue().set(
                PREFIX + hashed,
                data,
                props.getRefreshTtl(),
                TimeUnit.SECONDS
        );

        String userKey = USER_INDEX + userId;

        redisTemplate.opsForSet().add(userKey, hashed);
        return rawToken;
    }

    public UUID rotate(
            String rawToken,
            String deviceHash,
            String currentAccessJti
    ) {

        String hashed = hasher.hash(rawToken);
        String key = PREFIX + hashed;

        RefreshTokenData data = (RefreshTokenData) redisTemplate.opsForValue().get(key);

        if (data == null) {
            throw new RuntimeException("Refresh token reused or revoked");
        }

        if (!data.getDeviceHash().equals(deviceHash)) {
            throw new RuntimeException("Device mismatch");
        }

        if (!data.getParentJti().equals(currentAccessJti)) {
            throw new RuntimeException("Session revoked");
        }

        redisTemplate.delete(key);

        redisTemplate.opsForSet()
                .remove(USER_INDEX + data.getUserId(), hashed);

        return data.getUserId();
    }
}
