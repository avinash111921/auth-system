package com.risk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import tools.jackson.core.JacksonException;

/**
    Transient failures (Redis blip) are retried with back-off;
    malformed events go straight to the dead-letter topic.
    Either way a bad record never blocks its partition.
 */
@Configuration
class KafkaConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        var backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        var handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class, JacksonException.class);
        return handler;
    }
}
