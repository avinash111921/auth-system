package com.identity.service;

import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
    Every login attempt is paid for before the password is hashed,
    so guessing is capped per
    account and per source IP and an attack can't spend our bcrypt budget.
    A success clears the account's count and refunds the IP's;
    failures stay counted until the window expires.
 */

@Component
@RequiredArgsConstructor
public class LoginThrottle {

    private static final int MAX_ACCOUNT_ATTEMPTS = 5;
    private static final int MAX_IP_ATTEMPTS = 50;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final AttemptCounter attempts;

    public void acquire(String email, String ip) {
        String account = accountKey(email);
        if (!attempts.tryAcquire(account, MAX_ACCOUNT_ATTEMPTS, WINDOW)) {
            throw new AuthException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
        if (!attempts.tryAcquire(ipKey(ip), MAX_IP_ATTEMPTS, WINDOW)) {
            attempts.release(account);
            throw new AuthException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }

    public void succeeded(String email, String ip) {
        attempts.clear(accountKey(email));
        attempts.release(ipKey(ip));
    }

    /** After a password reset the account starts with a clean slate. */
    public void reset(String email) {
        attempts.clear(accountKey(email));
    }

    // Two keys in different hash slots, so two single-key scripts rather than one multi-key one.
    private static String accountKey(String email) {
        return "identity:login-fail:account:" + email;
    }

    private static String ipKey(String ip) {
        return "identity:login-fail:ip:" + ip;
    }
}
