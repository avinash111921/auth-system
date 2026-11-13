package com.identity.outbox;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
    Records an event in the caller's database transaction.
    If that transaction rolls back, the event never existed;
    if it commits, {@link OutboxRelay} is guaranteed to publish it.
 */

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final EntityManager entityManager;
    private final JsonMapper json;

    @Transactional
    public void append(AuthEvent event) {
        entityManager.persist(OutboxEvent.of(event.type(), event.key(), json.writeValueAsString(event)));
    }
}
