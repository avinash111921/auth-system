package com.token_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;


@Configuration
@RequiredArgsConstructor
public class JwtEncoderConfig {

    private final JwkSource jwkSource;

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(jwkSource);
    }
}

