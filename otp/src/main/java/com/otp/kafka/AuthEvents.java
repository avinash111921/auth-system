package com.otp.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
    The identity-service events this service reacts to;
    unknown fields are ignored so producers can add them.
 */

final class AuthEvents {

    static final String TOPIC = "auth.events.v1";

    private AuthEvents() {}

    @JsonIgnoreProperties(ignoreUnknown=true)
    record VerificationRequested(String registrationId, String email) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountExistsNotice(String email) {}

    @JsonIgnoreProperties(ignoreUnknown=true)
    record PasswordResetRequested(String email) {}

    @JsonIgnoreProperties(ignoreUnknown=true)
    record LoginChallengeRequired(String challengeId, String email) {}
}
