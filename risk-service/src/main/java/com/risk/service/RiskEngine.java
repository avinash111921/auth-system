package com.risk.service;

import com.risk.entity.RiskResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskEngine {

    private final StringRedisTemplate redis;

    private static final int NEW_DEVICE_SCORE = 30;
    private static final int NEW_IP_SCORE = 20;
    private static final int ODD_TIME_SCORE = 10;
    private static final int FAILURE_SCORE = 40;

    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration KNOWN_DEVICE_TTL = Duration.ofDays(90);
    private static final Duration KNOWN_IP_TTL = Duration.ofDays(30);
    private static final Duration SEEN_TTL = Duration.ofDays(365);

    public RiskResult calculateRisk(String userId, String deviceId, String ip) {

        if (isFirstLogin(userId)) {
            return new RiskResult(0, List.of("FIRST_LOGIN_BASELINE"));
        }

        int risk = 0;
        List<String> reasons = new ArrayList<>();

        if (isNewDevice(userId, deviceId)) {
            risk += NEW_DEVICE_SCORE;
            reasons.add("NEW_DEVICE");
        }

        if (isNewIp(userId, ip)) {
            risk += NEW_IP_SCORE;
            reasons.add("NEW_IP");
        }

        if (isOddTime()) {
            risk += ODD_TIME_SCORE;
            reasons.add("ODD_TIME");
        }

        if (recentFailures(userId)) {
            risk += FAILURE_SCORE;
            reasons.add("RECENT_FAILURES");
        }

        return new RiskResult(risk, reasons);
    }

    public void onSuccessfulLogin(String userId, String deviceId, String ip) {
        trustContext(userId, deviceId, ip);
        markUserSeen(userId);
        clearFailures(userId);
    }

    public void recordFailure(String userId) {
        String key = failuresKey(userId);
        redis.opsForValue().increment(key);
        redis.expire(key, FAILURE_WINDOW);
    }

    private boolean recentFailures(String userId) {
        String val = redis.opsForValue().get(failuresKey(userId));
        return val != null && Integer.parseInt(val) >= FAILURE_THRESHOLD;
    }

    private void clearFailures(String userId) {
        redis.delete(failuresKey(userId));
    }

    private void trustContext(String userId, String deviceId, String ip) {
        redis.opsForSet().add(knownDevicesKey(userId), deviceId);
        redis.opsForSet().add(knownIpsKey(userId), ip);
        redis.expire(knownDevicesKey(userId), KNOWN_DEVICE_TTL);
        redis.expire(knownIpsKey(userId), KNOWN_IP_TTL);
    }

    private boolean isNewDevice(String userId, String deviceId) {
        return !Boolean.TRUE.equals(redis.opsForSet().isMember(knownDevicesKey(userId), deviceId));
    }

    private boolean isNewIp(String userId, String ip) {
        return !Boolean.TRUE.equals(redis.opsForSet().isMember(knownIpsKey(userId), ip));
    }

    private boolean isFirstLogin(String userId) {
        return !Boolean.TRUE.equals(redis.hasKey(seenKey(userId)));
    }

    private void markUserSeen(String userId) {
        redis.opsForValue().set(seenKey(userId), "1", SEEN_TTL);
    }

    private static String failuresKey(String userId) {
        return "risk:fail:" + userId;
    }

    private static String knownDevicesKey(String userId) {
        return "risk:known-device:" + userId;
    }

    private static String knownIpsKey(String userId) {
        return "risk:known-ip:" + userId;
    }

    private static String seenKey(String userId) {
        return "risk:seen:" + userId;
    }

    private boolean isOddTime() {
        int hour = LocalDateTime.now().getHour();
        return hour < 6 || hour > 22;
    }
}
