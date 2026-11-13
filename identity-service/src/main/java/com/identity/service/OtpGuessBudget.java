package com.identity.service;

import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
    Caps OTP guesses per account across every code issued to it.
    Each code already burns after 5 misses, but asking for a new code started a
    fresh 5, so with 10 sends an hour an attacker got 50 guesses an hour at a 6-digit
    code. With this budget it is 5 an hour, whatever the sends.
 */
@Component
@RequiredArgsConstructor
public class OtpGuessBudget {

    private static final int MAX_GUESSES = 5;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final AttemptCounter attempts;

    public void spend(String purpose, String account) {
        if (!attempts.tryAcquire(key(purpose, account), MAX_GUESSES, WINDOW)) {
            throw new AuthException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }

    public void clear(String purpose, String account) {
        attempts.clear(key(purpose, account));
    }

    private static String key(String purpose, String account) {
        return "identity:otp-guesses:" + purpose + ":" + account;
    }
}
