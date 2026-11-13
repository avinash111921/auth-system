package com.identity.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** This service's identity when calling other services, and the secret it signs calls with. */
@ConfigurationProperties("service-auth")
public record ServiceAuthProperties(String serviceName, String secret) {
}
