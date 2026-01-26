package com.otp.service;

import com.otp.dto.OtpType;
import com.otp.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpGenerator generator;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    @Value("${otp.ttl}")
    private long ttl;
    private static final int MAX_ATTEMPTS = 5;

    public boolean verifyOtp(String key, String otp) {
        System.out.println("VERIFY OTP KEY = " + key);

        String storedHash = redisTemplate.opsForValue().get(key + ":hash");
        String attemptsStr = redisTemplate.opsForValue().get(key + ":attempts");

        System.out.println("stored hash " + storedHash);

        if (storedHash == null) {
            throw new IllegalStateException("OTP expired or not found");
        }

        int attempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);

        if (attempts >= MAX_ATTEMPTS) {
            deleteOtp(key);
            throw new IllegalStateException("Too many attempts");
        }
        System.out.println("HASH OTP "+ hash(otp));
        if (!hash(otp).equals(storedHash)) {
            redisTemplate.opsForValue().increment(key + ":attempts");
            return false;
        }

        deleteOtp(key);
        return true;
    }

    public void generateAndSendOtp(String email, OtpType type) {
        String otp = generator.generate();
        String key = buildKey(email, type);
        saveOtp(otp, key);
        emailService.send(email, otp);
    }

    public void generateAndSendLoginOtp(String email,String challengeId){
        String otp = generator.generate();
        String key = buildLoginKey(challengeId);
        saveOtp(otp,key);
        emailService.send(email, otp);
    }

    private void saveOtp(String otp,String key) {

        System.out.println("SAVING OTP KEY = " + key);
        redisTemplate.opsForValue().set(
                key + ":hash",
                hash(otp),
                ttl,
                TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                key + ":attempts",
                "0",
                ttl,
                TimeUnit.SECONDS
        );
    }

    private void deleteOtp(String key) {
        redisTemplate.delete(key + ":hash");
        redisTemplate.delete(key + ":attempts");
    }

    public String buildKey(String email, OtpType type) {
        return "otp:" + type.name() + ":" + email.toLowerCase();
    }

    public String buildLoginKey(String challengeId){
        return "otp:LOGIN:" + challengeId;
    }

    private String hash(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("OTP hashing failed", e);
        }
    }

}
