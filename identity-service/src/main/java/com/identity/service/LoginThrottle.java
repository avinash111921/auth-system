package com.identity.service;

import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Checked before the password is hashed, so guessing is capped per account and per source IP
 * and an attack can't spend our bcrypt budget. Counters expire on their own: no permanent lockout.
 */
@Component
@RequiredArgsConstructor
public class LoginThrottle {

    private static final int MAX_ACCOUNT_FAILURES = 5;
    private static final int MAX_IP_FAILURES = 50;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    public void checkAllowed(String email, String ip) {
        if (count(accountKey(email)) >= MAX_ACCOUNT_FAILURES || count(ipKey(ip)) >= MAX_IP_FAILURES) {
            throw new AuthException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }

    public void recordFailure(String email, String ip) {
        increment(accountKey(email));
        increment(ipKey(ip));
    }

    public void reset(String email) {
        redis.delete(accountKey(email));
    }

    private long count(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    private void increment(String key) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, WINDOW);
        }
    }

    private static String accountKey(String email) {
        return "identity:login-fail:account:" + email;
    }

    private static String ipKey(String ip) {
        return "identity:login-fail:ip:" + ip;
    }
}
