package com.token_service.key;

import com.nimbusds.jose.jwk.RSAKey;
import com.token_service.entity.StoredKey;
import com.token_service.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 <pre>
    jwt:keys:{kid}    StoredKey JSON (private JWK sealed, public JWK in clear)
    jwt:kids    set of every retained kid: the index JWKS reads instead of KEYS jwt:keys:*
    jwt:active-kid    the kid new tokens are signed with
 </pre>
    KEYS is O(n) over the whole database, and DB 0 also holds every
    refresh token, so the old JWKS endpoint got slower with every
    session and blocked Redis while it ran.
 */

@Service
@RequiredArgsConstructor
public class RedisKeyStore {

    private static final String KEY_PREFIX = "jwt:keys:";
    private static final String KIDS = "jwt:kids";
    private static final String ACTIVE_KID = "jwt:active-kid";

    private final StringRedisTemplate redis;
    private final KeyEncryption encryption;
    private volatile RSAKey cachedActiveKey;

    /**
        One-time migration for keys written before the index existed.
        SCAN, not KEYS: it doesn't block Redis.
     */
    @PostConstruct
    void indexExistingKeys() {
        if (Boolean.TRUE.equals(redis.hasKey(KIDS))) {
            return;
        }
        ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(1_000).build();
        try (Cursor<String> keys = redis.scan(options)) {
            keys.forEachRemaining(key -> redis.opsForSet().add(KIDS, key.substring(KEY_PREFIX.length())));
        }
    }

    public void storeKey(RSAKey key) {
        StoredKey stored = new StoredKey(
                encryption.encrypt(key.toJSONString()),
                key.toPublicJWK().toJSONString(),
                Instant.now());
        redis.opsForValue().set(KEY_PREFIX + key.getKeyID(), JsonUtils.toJson(stored));
        redis.opsForSet().add(KIDS, key.getKeyID());
        redis.opsForValue().set(ACTIVE_KID, key.getKeyID());
    }

    public RSAKey getActiveKey() {
        String kid = getActiveKid();
        if (kid == null) {
            return null;
        }
        RSAKey cached = cachedActiveKey;
        if (cached != null && kid.equals(cached.getKeyID())) {
            return cached;
        }
        RSAKey loaded = getKey(kid);
        cachedActiveKey = loaded;
        return loaded;
    }

    public String getActiveKid() {
        return redis.opsForValue().get(ACTIVE_KID);
    }

    public RSAKey getKey(String kid) {
        String json = redis.opsForValue().get(KEY_PREFIX + kid);
        return json == null ? null : parse(encryption.decrypt(JsonUtils.fromJson(json, StoredKey.class).getJwkJson()));
    }

    /**
        Metadata for every retained key, by kid.
     */
    public Map<String, StoredKey> getAllKeys() {
        List<String> kids = new ArrayList<>(members());
        Map<String, StoredKey> result = new LinkedHashMap<>();
        if (kids.isEmpty()) {
            return result;
        }
        List<String> values = redis.opsForValue().multiGet(kids.stream().map(kid -> KEY_PREFIX + kid).toList());
        for (int i = 0; i < kids.size(); i++) {
            String json = values == null ? null : values.get(i);
            if (json == null) {
                redis.opsForSet().remove(KIDS, kids.get(i));
            } else {
                result.put(kids.get(i), JsonUtils.fromJson(json, StoredKey.class));
            }
        }
        return result;
    }

    /**
        The JWKS document's keys: public parameters only.
     */
    public List<Map<String, Object>> publicJwks() {
        return getAllKeys().values().stream()
                .map(stored -> stored.getPublicJwkJson() != null
                        ? parse(stored.getPublicJwkJson())
                        : parse(encryption.decrypt(stored.getJwkJson())).toPublicJWK())
                .map(RSAKey::toJSONObject)
                .toList();
    }

    public void deleteKey(String kid) {
        redis.delete(KEY_PREFIX + kid);
        redis.opsForSet().remove(KIDS, kid);
    }

    private Set<String> members() {
        Set<String> kids = redis.opsForSet().members(KIDS);
        return kids == null ? Set.of() : kids;
    }

    private static RSAKey parse(String json) {
        try {
            return RSAKey.parse(json);
        } catch (ParseException e) {
            throw new IllegalStateException("Invalid JWK stored in Redis", e);
        }
    }
}
