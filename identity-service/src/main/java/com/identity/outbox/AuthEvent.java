package com.identity.outbox;

import java.util.UUID;

/**
    Everything identity-service tells the rest of the platform.
    {@link #key()} is the Kafka record key, so all events about one user
    (or one pending registration) stay in order on one partition.
    {@link #type()} travels as a header and is versioned for safe
    evolution.
 */

public sealed interface AuthEvent {

    String TOPIC = "auth.events.v1";
    String type();
    String key();

    record VerificationRequested(String registrationId, String email) implements AuthEvent {
        public String type() {
            return "verification_requested.v1";
        }
        public String key() {
            return registrationId;
        }
    }

    record AccountExistsNotice(String email) implements AuthEvent {
        public String type() {
            return "account_exists_notice.v1";
        }
        public String key() {
            return email;
        }
    }

    record PasswordResetRequested(UUID userId, String email) implements AuthEvent {
        public String type() {
            return "password_reset_requested.v1";
        }
        public String key() {
            return userId.toString();
        }
    }

    record LoginChallengeRequired(String challengeId, UUID userId, String email) implements AuthEvent {
        public String type() {
            return "login_challenge_required.v1";
        }
        public String key() {
            return userId.toString();
        }
    }

    record LoginSucceeded(UUID userId, String ip, String deviceId) implements AuthEvent {
        public String type() {
            return "login_succeeded.v1";
        }
        public String key() {
            return userId.toString();
        }
    }

    record LoginFailed(UUID userId, String ip, String deviceId) implements AuthEvent {
        public String type() {
            return "login_failed.v1";
        }
        public String key() {
            return userId.toString();
        }
    }

    record LoginBlocked(UUID userId, String ip, String deviceId, int riskScore) implements AuthEvent {
        public String type() {
            return "login_blocked.v1";
        }
        public String key() {
            return userId.toString();
        }
    }
}
