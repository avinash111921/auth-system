package com.token_service.service;

import com.token_service.config.JwtProperties;
import com.token_service.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
    Refresh tokens are single-use and grouped into families, one per login.
    Rotation consumes the presented token atomically and issues its
    successor in the same family.
    Presenting a token that was already consumed means two parties hold it,
    so the whole family is revoked.

    <pre>
        token:{hash}:rt               userId|familyId|deviceId   TTL = family's remaining lifetime
        token:{hash}:used             familyId                   TTL = refresh TTL
        token:family:{familyId}       userId                     TTL = absolute session lifetime
        token:user:{userId}:families  set of familyId
    </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshSessions {

    private static final RedisScript<String> CONSUME =
            RedisScript.of(new ClassPathResource("redis/consume-refresh-token.lua"), String.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final TokenHasher hasher;
    private final JwtProperties props;

    public String start(UUID userId, String deviceId) {
        String familyId = UUID.randomUUID().toString();
        Duration lifetime = Duration.ofSeconds(props.getRefreshTtl());

        redis.opsForValue().set(familyKey(familyId), userId.toString(), lifetime);
        redis.opsForSet().add(userFamiliesKey(userId), familyId);
        redis.expire(userFamiliesKey(userId), lifetime);

        return mint(new RefreshSession(userId, familyId, deviceId), lifetime);
    }

    public Rotation rotate(String rawToken, String deviceId) {
        String hash = hasher.hash(rawToken);
        String stored = consume(hash);

        if (stored == null) {
            String reusedFamily = redis.opsForValue().get(usedKey(hash));
            if (reusedFamily != null) {
                log.warn("refresh token replayed; revoking its family");
                revokeFamily(reusedFamily);
            }
            throw new InvalidRefreshTokenException();
        }

        RefreshSession session = RefreshSession.decode(stored);
        Long remaining = redis.getExpire(familyKey(session.familyId()));
        if (!session.deviceId().equals(deviceId) || remaining == null || remaining <= 0) {
            revokeFamily(session.familyId());
            throw new InvalidRefreshTokenException();
        }
        return new Rotation(session.userId(), mint(session, Duration.ofSeconds(remaining)));
    }

    /**
        Holding the refresh token is enough authority to end its own session. */
    public void revoke(String rawToken) {
        String stored = consume(hasher.hash(rawToken));
        if (stored != null) {
            revokeFamily(RefreshSession.decode(stored).familyId());
        }
    }

    public void revokeAll(UUID userId) {
        Set<String> families = redis.opsForSet().members(userFamiliesKey(userId));
        if (families != null && !families.isEmpty()) {
            redis.delete(families.stream().map(RefreshSessions::familyKey).toList());
        }
        redis.delete(userFamiliesKey(userId));
    }

    private String consume(String hash) {
        return redis.execute(CONSUME, List.of(tokenKey(hash), usedKey(hash)), String.valueOf(props.getRefreshTtl()));
    }

    // Tokens of a revoked family are left to expire;
    // rotate() rejects them because the family key is gone.
    private void revokeFamily(String familyId) {
        String userId = redis.opsForValue().getAndDelete(familyKey(familyId));
        if (userId != null) {
            redis.opsForSet().remove(userFamiliesKey(UUID.fromString(userId)), familyId);
        }
    }

    private String mint(RefreshSession session, Duration ttl) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(tokenKey(hasher.hash(rawToken)), session.encode(), ttl);
        return rawToken;
    }

    // The {hash} tag keeps a token and its used-marker in one Redis Cluster slot, as the Lua script requires.
    private static String tokenKey(String hash) {
        return "token:{" + hash + "}:rt";
    }

    private static String usedKey(String hash) {
        return "token:{" + hash + "}:used";
    }

    private static String familyKey(String familyId) {
        return "token:family:" + familyId;
    }

    private static String userFamiliesKey(UUID userId) {
        return "token:user:" + userId + ":families";
    }

    public record Rotation(UUID userId, String refreshToken) {
    }
}
