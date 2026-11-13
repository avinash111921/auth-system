package com.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class LoginChallengeStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    void save(String challengeId, LoginChallenge challenge) {
        redis.opsForValue().set(key(challengeId), json.writeValueAsString(challenge), TTL);
    }

    Optional<LoginChallenge> find(String challengeId) {
        return Optional.ofNullable(redis.opsForValue().get(key(challengeId))).map(this::parse);
    }

    /**
        GET DEL: a challenge completes at most once,
        even under concurrent OTP submissions.
     */
    Optional<LoginChallenge> take(String challengeId) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(key(challengeId))).map(this::parse);
    }

    private LoginChallenge parse(String value) {
        return json.readValue(value, LoginChallenge.class);
    }

    private static String key(String challengeId) {
        return "identity:login-challenge:" + challengeId;
    }
}
