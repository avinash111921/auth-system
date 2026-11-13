package com.identity.dto;

import com.identity.entity.User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(UUID id, String email, String fullName, Instant memberSince,
                         Instant tokenIssuedAt, Instant tokenExpiresAt) {

    public static MeResponse from(User user, Jwt token) {
        return new MeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt(),
                token.getIssuedAt(), token.getExpiresAt());
    }
}
