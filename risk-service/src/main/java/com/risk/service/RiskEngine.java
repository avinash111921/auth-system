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

    public RiskResult calculateRisk(String userId, String deviceFp, String ip) {

        if (isFirstLogin(userId)) {
            return new RiskResult(0, List.of("FIRST_LOGIN_BASELINE"));
        }

        int risk = 0;
        List<String> reasons = new ArrayList<>();

        if (isNewDevice(userId, deviceFp)) {
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

    public void onSuccessfulLogin(String userId, String deviceFp, String ip) {
        trustContext(userId, deviceFp, ip);
        markUserSeen(userId);
        clearFailures(userId);
    }

    public void recordFailure(String userId) {
        String key = "login:fail:" + userId;
        redis.opsForValue().increment(key);
        redis.expire(key, FAILURE_WINDOW);
    }

    private boolean recentFailures(String userId) {
        String val = redis.opsForValue().get("login:fail:" + userId);
        return val != null && Integer.parseInt(val) >= FAILURE_THRESHOLD;
    }

    private void clearFailures(String userId) {
        redis.delete("login:fail:" + userId);
    }

    private void trustContext(String userId, String fp, String ip) {
        redis.opsForSet().add("known:device:" + userId, fp);
        redis.opsForSet().add("known:ip:" + userId, ip);

        redis.expire("known:device:" + userId, Duration.ofDays(90));
        redis.expire("known:ip:" + userId, Duration.ofDays(30));
    }

    private boolean isNewDevice(String userId, String fp) {
        return !Boolean.TRUE.equals(
                redis.opsForSet().isMember("known:device:" + userId, fp)
        );
    }

    private boolean isNewIp(String userId, String ip) {
        return !Boolean.TRUE.equals(
                redis.opsForSet().isMember("known:ip:" + userId, ip)
        );
    }

    private boolean isFirstLogin(String userId) {
        return !Boolean.TRUE.equals(redis.hasKey("user:seen:" + userId));
    }

    private void markUserSeen(String userId) {
        redis.opsForValue().set("user:seen:" + userId, "1");
    }

    private boolean isOddTime() {
        int hour = LocalDateTime.now().getHour();
        return hour < 6 || hour > 22;
    }
}
