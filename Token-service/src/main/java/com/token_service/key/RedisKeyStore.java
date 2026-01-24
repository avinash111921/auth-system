package com.token_service.key;

import com.nimbusds.jose.jwk.RSAKey;
import com.token_service.entity.StoredKey;
import com.token_service.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisKeyStore {

    private static final String KEY_PREFIX = "jwt:keys:";
    private static final String ACTIVE_KID = "jwt:active-kid";

    private final StringRedisTemplate redis;

    public void storeKey(RSAKey key) {

        StoredKey stored = new StoredKey(
                key.toJSONString(),
                Instant.now()
        );

        redis.opsForValue().set(
                KEY_PREFIX + key.getKeyID(),
                JsonUtils.toJson(stored)
        );

        redis.opsForValue().set(
                ACTIVE_KID,
                key.getKeyID()
        );
    }

    public RSAKey getActiveKey() {
        String kid = getActiveKid();
        return kid == null ? null : getKey(kid);
    }

    public String getActiveKid() {
        return redis.opsForValue().get(ACTIVE_KID);
    }

    public RSAKey getKey(String kid) {

        String json = redis.opsForValue().get(KEY_PREFIX + kid);
        if (json == null) return null;

        StoredKey stored = JsonUtils.fromJson(json, StoredKey.class);

        try {
            return RSAKey.parse(stored.jwkJson());
        } catch (ParseException e) {
            throw new IllegalStateException("Invalid JWK stored in Redis", e);
        }
    }
    public Map<String, StoredKey> getAllKeys() {

        Set<String> redisKeys = redis.keys(KEY_PREFIX + "*");
        Map<String, StoredKey> result = new HashMap<>();

        if (redisKeys == null) return result;

        for (String redisKey : redisKeys) {
            String json = redis.opsForValue().get(redisKey);
            if (json == null) continue;

            StoredKey stored = JsonUtils.fromJson(json, StoredKey.class);
            result.put(
                    redisKey.replace(KEY_PREFIX, ""),
                    stored
            );
        }
        return result;
    }

    public void deleteKey(String kid) {
        redis.delete(KEY_PREFIX + kid);
    }
}
