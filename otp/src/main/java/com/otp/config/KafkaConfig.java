package com.otp.config;

import com.fasterxml.jackson.core.JacksonException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {
    @Bean
    DefaultErrorHandler KafkaErrorHandle(KafkaTemplate<Object,Object> template) {
        var backoff=new ExponentialBackOffWithMaxRetries(5);
        backoff.setInitialInterval(1_000);
        backoff.setMultiplier(2.0);
        var handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template),backoff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class, JacksonException.class);
        return handler;
    }
}
