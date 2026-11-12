package com.otp.kafka;

import com.otp.kafka.AuthEvents.AccountExistsNotice;
import com.otp.kafka.AuthEvents.LoginChallengeRequired;
import com.otp.kafka.AuthEvents.PasswordResetRequested;
import com.otp.kafka.AuthEvents.VerificationRequested;
import com.otp.service.EmailService;
import com.otp.service.OtpPurpose;
import com.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * The outbox delivers at least once, so each event ID is remembered after it is handled and a
 * redelivery doesn't email a second code. Events meant for other consumers are skipped.
 */
@Component
@RequiredArgsConstructor
class AuthEventListener {

    private static final Duration PROCESSED_TTL = Duration.ofDays(7);

    private final OtpService otpService;
    private final EmailService emailService;
    private final StringRedisTemplate redis;
    private final JsonMapper json;

    @KafkaListener(topics = AuthEvents.TOPIC, groupId = "otp-service")
    void onEvent(ConsumerRecord<String, String> record) {
        String eventId = header(record, "event-id");
        if (Boolean.TRUE.equals(redis.hasKey(processedKey(eventId)))) {
            return;
        }
        switch (header(record, "event-type")) {
            case "verification_requested.v1" -> {
                var event = json.readValue(record.value(), VerificationRequested.class);
                otpService.issue(OtpPurpose.REGISTRATION, event.registrationId(), event.email());
            }
            case "password_reset_requested.v1" -> {
                var event = json.readValue(record.value(), PasswordResetRequested.class);
                otpService.issue(OtpPurpose.PASSWORD_RESET, event.email(), event.email());
            }
            case "login_challenge_required.v1" -> {
                var event = json.readValue(record.value(), LoginChallengeRequired.class);
                otpService.issue(OtpPurpose.LOGIN, event.challengeId(), event.email());
            }
            case "account_exists_notice.v1" ->
                    emailService.sendAccountExistsNotice(json.readValue(record.value(), AccountExistsNotice.class).email());
            default -> {
                return;
            }
        }
        redis.opsForValue().set(processedKey(eventId), "1", PROCESSED_TTL);
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("missing header " + name);
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static String processedKey(String eventId) {
        return "otp:event:" + eventId;
    }
}
