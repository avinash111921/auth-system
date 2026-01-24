package com.token_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer;
    private long accessTtl;
    private long refreshTtl;
    private KeyRotation keyRotation;

    @Data
    public static class KeyRotation {
        private int rotateEveryHours;
        private int retainOldKeysHours;
    }
}