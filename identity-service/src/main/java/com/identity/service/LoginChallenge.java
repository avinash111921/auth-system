package com.identity.service;

import java.util.UUID;

record LoginChallenge(UUID userId, String deviceId) {
}
