package com.token_service.service;

import com.token_service.config.JwtProperties;
import com.token_service.entity.AccessTokenResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    public AccessTokenResult generateAccessToken(UUID userId, String deviceHash) {

        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(props.getAccessTtl()))
                .id(jti)
                .claim("device", deviceHash)
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        return new AccessTokenResult(token, jti);
    }
}

