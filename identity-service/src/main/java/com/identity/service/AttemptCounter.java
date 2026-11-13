package com.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
    Fixed-window attempt budgets in Redis.
    Checking and counting happen in one Lua call, so
    fifty parallel requests can't all read "4 of 5" and all get through.
 */

@Component
@RequiredArgsConstructor
public class AttemptCounter {

    private static final RedisScript<Long> ACQUIRE =
            RedisScript.of(new ClassPathResource("redis/attempt-acquire.lua"), Long.class);
    private static final RedisScript<Long> RELEASE =
            RedisScript.of(new ClassPathResource("redis/attempt-release.lua"), Long.class);

    private final StringRedisTemplate redis;

    /**
        Counts one attempt; false means the budget is spent and nothing
        was counted.
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        Long count = redis.execute(ACQUIRE, List.of(key), String.valueOf(limit), String.valueOf(window.toMillis()));
        return count != null && count > 0;
    }

    /**
        Hands one attempt back, e.g. when the attempt it paid for succeeded.
     */
    public void release(String key) {
        redis.execute(RELEASE, List.of(key));
    }

    public void clear(String key) {
        redis.delete(key);
    }
}
