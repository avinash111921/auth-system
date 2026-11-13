package com.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PendingRegistrationStore {

    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    void save(String registrationId, PendingRegistration registration) {
        redis.opsForValue().set(key(registrationId), json.writeValueAsString(registration), TTL);
    }

    /** GET DEL: of two concurrent completions of the same registration, exactly one gets the row. */
    Optional<PendingRegistration> take(String registrationId) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(key(registrationId)))
                .map(value -> json.readValue(value, PendingRegistration.class));
    }

    private static String key(String registrationId) {
        return "identity:registration:" + registrationId;
    }
}
