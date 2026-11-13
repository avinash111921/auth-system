package com.token_service.controller;

import com.token_service.key.RedisKeyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/.well-known")
public class JwksController {

    private final RedisKeyStore keyStore;

    @GetMapping("/jwks.json")
    public Map<String, List<Map<String, Object>>> jwks() {
        return Map.of("keys", keyStore.publicJwks());
    }
}
