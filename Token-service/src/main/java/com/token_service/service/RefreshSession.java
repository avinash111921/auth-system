package com.token_service.service;

import java.util.UUID;

/**
    Stored as "userId|familyId|deviceId";
    the device ID is last, so it may contain anything.
 */

record RefreshSession(UUID userId, String familyId, String deviceId) {

    String encode() {
        return userId + "|" + familyId + "|" + deviceId;
    }

    static RefreshSession decode(String value) {
        String[] parts = value.split("\\|", 3);
        return new RefreshSession(UUID.fromString(parts[0]), parts[1], parts[2]);
    }
}
