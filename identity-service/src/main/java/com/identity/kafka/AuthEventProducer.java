package com.identity.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String eventType, Object payload) {
            kafkaTemplate.send("auth-event",eventType,payload);
    }
}