package com.risk.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.risk.service.RiskEngine;
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
   Login outcomes arrive here from identity-service's outbox rather than as synchronous RPCs.
   Event IDs are remembered so a redelivered failure isn't counted twice.
 */

@Component
@RequiredArgsConstructor
class AuthEventListener {

    private static final String TOPIC = "auth.events.v1";
    private static final Duration PROCESSED_TTL = Duration.ofDays(7);

    private final RiskEngine engine;
    private final StringRedisTemplate redis;
    private final JsonMapper json;

    @KafkaListener(topics = TOPIC, groupId = "risk-service")
    void onEvent(ConsumerRecord<String, String> record) {
        String eventId = header(record, "event-id");
        if (Boolean.TRUE.equals(redis.hasKey(processedKey(eventId)))) {
            return;
        }
        switch (header(record, "event-type")) {
            case "login_succeeded.v1" -> {
                var event = json.readValue(record.value(), LoginOutcome.class);
                engine.onSuccessfulLogin(event.userId(), event.deviceId(), event.ip());
            }
            case "login_failed.v1" -> engine.recordFailure(json.readValue(record.value(), LoginOutcome.class).userId());
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
        return "risk:event:" + eventId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LoginOutcome(String userId, String ip, String deviceId) {
    }
}
