package com.risk.kafka;

import com.risk.service.RiskEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskEventListener {

    private final RiskEngine riskEngine;

    @KafkaListener(
            topics = "auth-event",
            groupId = "risk-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleEvent(
            Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String eventType,
            Acknowledgment ack
    ) {
        switch (eventType) {

            case "LOGIN_FAILED" -> {
                String userId = payload.get("userId").toString();
                riskEngine.recordFailure(userId);
            }
        }
    }
}
