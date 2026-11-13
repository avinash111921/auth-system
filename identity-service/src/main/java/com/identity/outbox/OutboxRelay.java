package com.identity.outbox;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
    Moves committed outbox rows to Kafka.
    Delivery is at-least-once: a crash between the broker
    ack and our commit republishes the batch,
    so consumers de-duplicate on the event-id header.
 */

@Component
@RequiredArgsConstructor
class OutboxRelay {

    private static final int BATCH_SIZE = 200;
    private static final Duration RETENTION = Duration.ofDays(7);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outbox.lockPending(BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        CompletableFuture<?>[] sends = batch.stream()
                .map(event -> kafka.send(toRecord(event)))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(sends).join();

        Instant now = Instant.now();
        batch.forEach(event -> event.markPublished(now));
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgePublished() {
        outbox.deletePublishedBefore(Instant.now().minus(RETENTION));
    }

    private static ProducerRecord<String, String> toRecord(OutboxEvent event) {
        var record = new ProducerRecord<>(AuthEvent.TOPIC, event.getKey(), event.getPayload());
        record.headers().add("event-id", event.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event-type", event.getType().getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
