package com.identity.util;

import com.identity.dto.LoginChallenge;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginChallengeStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration TTL = Duration.ofMinutes(5);

    public void save(LoginChallenge challenge) {
        redisTemplate.opsForValue().set(
                key(challenge.getChallengeId()),
                challenge,
                TTL
        );
    }

    public LoginChallenge get(String challengeId) {
        return (LoginChallenge) redisTemplate.opsForValue().get(key(challengeId));
    }

    public void delete(String challengeId) {
        redisTemplate.delete(key(challengeId));
    }

    private String key(String challengeId) {
        return "login:challenge:" + challengeId;
    }
}
