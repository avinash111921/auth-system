package com.token_service.controller;

import com.nimbusds.jose.jwk.RSAKey;
import com.token_service.entity.StoredKey;
import com.token_service.key.RedisKeyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/.well-known")
public class JwksController {

    private final RedisKeyStore keyStore;

    @GetMapping("/jwks.json")
    public Map<String, Object> jwks() {

        List<Map<String, Object>> keys =
                keyStore.getAllKeys().values().stream()
                        .map(this::toPublicJwk)
                        .toList();

        return Map.of("keys", keys);
    }

    private Map<String, Object> toPublicJwk(StoredKey storedKey) {
        try {
            RSAKey rsaKey = RSAKey.parse(storedKey.getJwkJson());
            return rsaKey.toPublicJWK().toJSONObject();
        } catch (ParseException e) {
            throw new IllegalStateException(
                    "Invalid JWK stored in Redis",
                    e
            );
        }
    }
}
