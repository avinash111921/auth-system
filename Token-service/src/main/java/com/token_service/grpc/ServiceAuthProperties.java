package com.token_service.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

/** Internal callers this service accepts, keyed by service name, with the gRPC services each may call. */
@ConfigurationProperties("service-auth")
public record ServiceAuthProperties(Map<String, TrustedCaller> trustedCallers) {

    public record TrustedCaller(String secret, Set<String> allowedServices) {
    }
}
