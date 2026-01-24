package com.otp.service;

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

    private final OtpService otpService;

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
        String email = payload.get("email").toString();

        switch (eventType) {
            case "USER_REGISTERED", "USER_LOGIN_OTP_REQUIRED" -> otpService.sendVerificationOtp(email);

            case "PASSWORD_RESET_REQUESTED" -> otpService.sendPasswordResetOtp(email);

            default ->
                    throw new IllegalArgumentException(
                            "Unknown event type: " + eventType
                    );
        }
        ack.acknowledge();
    }
}
