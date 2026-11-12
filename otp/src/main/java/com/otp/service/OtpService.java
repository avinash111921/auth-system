package com.otp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
    Codes are stored as HMAC-SHA256 under a server secret: with only a million possible codes,
    a plain hash would be reversed instantly by anyone who can read Redis.
    Neither codes nor recipients are ever logged.
 */

@Service
@Slf4j
public class OtpService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_SENDS_PER_RECIPIENT_PER_HOUR=10;
    private static final RedisScript<Long> VERIFY = RedisScript.of(new ClassPathResource("redis/otp-verify.lua"), Long.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final EmailService email;
    private final Duration ttl;
    private final SecretKeySpec hmacKey;

    public OtpService(StringRedisTemplate redis, EmailService email,
                      @Value("${otp.ttl}") long ttlSeconds,
                      @Value("${otp.hmac-secret}") String hmacSecret) {
        byte[] secret = hmacSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("otp.hmac-secret must be at least 32 bytes");
        }
        this.redis = redis;
        this.email = email;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.hmacKey = new SecretKeySpec(secret, "HmacSHA256");
    }

    public void issue(OtpPurpose purpose, String subject, String recipient) {
        if (!withinSendLimit(recipient)) {
            log.warn("OTP send limit reached for a recipient; {} code not sent", purpose);
            return;
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        String base = baseKey(purpose, subject);
        redis.delete(base + ":attempts");
        redis.opsForValue().set(base + ":hash", hmac(code), ttl);
        email.sendCode(recipient, purpose, code, ttl);
    }

    public boolean verify(OtpPurpose purpose, String subject, String code) {
        if (subject.isBlank() || code.isBlank()) {
            return false;
        }
        String base = baseKey(purpose, subject);
        Long result = redis.execute(VERIFY, List.of(base + ":hash", base + ":attempts"),
                hmac(code), String.valueOf(MAX_ATTEMPTS));
        return Long.valueOf(1).equals(result);
    }

    private boolean withinSendLimit(String recipient) {
        String key = "otp:sends:" + recipient.toLowerCase(Locale.ROOT);
        Long sends = redis.opsForValue().increment(key);
        if (sends != null && sends == 1) {
            redis.expire(key, Duration.ofHours(1));
        }
        return sends != null && sends <= MAX_SENDS_PER_RECIPIENT_PER_HOUR;
    }

    private static String baseKey(OtpPurpose purpose, String subject) {
        return "otp:{" + purpose.name() + ":" + subject + "}";
    }

    private String hmac(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
