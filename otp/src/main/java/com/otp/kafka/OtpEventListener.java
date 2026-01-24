package com.otp.kafka;

import com.otp.service.LoginOtpService;
import com.otp.service.PasswordResetOtpService;
import com.otp.service.RegisterOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtpEventListener {

    private final LoginOtpService loginOtpService;
    private final PasswordResetOtpService passwordResetOtpService;
    private final RegisterOtpService registerOtpService;

    @KafkaListener(
            topics = "auth-event",
            groupId = "otp-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAuthEvent(
            Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String eventType,
            Acknowledgment ack
    ) {
        switch (eventType) {
            case "USER_REGISTERED" -> {
                String email = payload.get("email").toString();
                registerOtpService.sendVerificationOtp(email);
            }

            case "PASSWORD_RESET_REQUESTED" -> {
                String email = payload.get("email").toString();
                passwordResetOtpService.sendPasswordResetOtp(email);
            }

            case "LOGIN_CHALLENGE_REQUIRED" -> {
                String email = payload.get("email").toString();
                String challengeId = payload.get("challengeId").toString();
                loginOtpService.sendLoginOtp(email, challengeId);
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unknown event type: " + eventType
                    );
        }
        ack.acknowledge();
    }
}
