package com.identity.config;

import com.identity.outbox.AuthEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
    identity-service owns the event topic,so it declares it.
    Auto-creation gave the broker default of one partition,
    which caps each consumer group at one active consumer.
    The dead-letter topic must have at least as many partitions:
    the recoverer writes a failed record to the same partition number
    it came from.
 */

@Configuration
class KafkaTopicsConfig {

    private final int partitions;
    private final short replicas;

    KafkaTopicsConfig(@Value("${auth.events.partitions:12}") int partitions,
                      @Value("${auth.events.replicas:1}") short replicas) {
        this.partitions = partitions;
        this.replicas = replicas;
    }

    @Bean
    NewTopic authEvents() {
        return TopicBuilder.name(AuthEvent.TOPIC).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic authEventsDeadLetter() {
        return TopicBuilder.name(AuthEvent.TOPIC + ".DLT").partitions(partitions).replicas(replicas).build();
    }
}
