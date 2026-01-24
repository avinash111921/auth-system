package com.token_service.entity;

import java.time.Instant;

public record StoredKey(
        String jwkJson,
        Instant createdAt
) {}
