package com.risk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiskEngine {

    private final StringRedisTemplate redis;

    public int calculateRisk(String userId, String deviceFp, String ip) {

        int risk = 0;

        if (isNewDevice(userId, deviceFp)) risk += 30;
        if (isNewIp(userId, ip)) risk += 20;
        if (isOddTime()) risk += 10;
        if (recentFailures(userId)) risk += 40;

        return risk;
    }

    private boolean isNewDevice(String userId, String fp) {
        return !redis.opsForSet().isMember("known:device:" + userId, fp);
    }

    private boolean isNewIp(String userId, String ip) {
        return !redis.opsForSet().isMember("known:ip:" + userId, ip);
    }

    private boolean isOddTime() {
        int hour = LocalDateTime.now().getHour();
        return hour < 6 || hour > 22;
    }

    private boolean recentFailures(String userId) {
        String val = redis.opsForValue().get("login:fail:" + userId);
        return val != null && Integer.parseInt(val) >= 3;
    }

    public void recordFailure(String userId) {
        String key = "login:fail:" + userId;
        redis.opsForValue().increment(key);
        redis.expire(key, Duration.ofMinutes(15));
    }

    public void trustContext(String userId, String fp, String ip) {
        redis.opsForSet().add("known:device:" + userId, fp);
        redis.opsForSet().add("known:ip:" + userId, ip);
    }
}
