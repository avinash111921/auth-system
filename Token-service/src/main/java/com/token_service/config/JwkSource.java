package com.token_service.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.token_service.key.RedisKeyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwkSource implements JWKSource<SecurityContext> {

    private final RedisKeyStore keyStore;

    @Override
    public List<JWK> get(
            JWKSelector jwkSelector,
            SecurityContext context
    ) {

        RSAKey activeKey = keyStore.getActiveKey();

        if (activeKey == null) {
            throw new IllegalStateException("No active JWT signing key available");
        }
        return jwkSelector.select(
                new JWKSet(activeKey)
        );
    }
}
// JWK = Key + Metadata + JSON
// Private keys never expose d, p, q in JWKS
// JWKSelector = logic that selects the correct JWK from a JWKS set to verify a JWT