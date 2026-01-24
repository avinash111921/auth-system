package com.token_service.service;

import com.nimbusds.jose.jwk.RSAKey;
import com.token_service.config.JwtProperties;
import com.token_service.key.RedisKeyStore;
import com.token_service.key.RsaKeyProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class KeyRotationService {

    private final RedisKeyStore keyStore;
    private final RsaKeyProvider keyProvider;
    private final JwtProperties props;

    @PostConstruct
    public void init() throws Exception {
        if (keyStore.getActiveKid() == null) {
            rotateKey();
        }
    }

    @Scheduled(fixedDelayString = "${jwt.key-rotation.rotate-delay-ms}")
    @SchedulerLock(name = "jwt-key-rotation")
    public void rotateKey() throws Exception {
        RSAKey key = keyProvider.generateNewKey();
        keyStore.storeKey(key);
    }

    @Scheduled(fixedDelayString = "${jwt.key-rotation.cleanup-delay-ms}")
    @SchedulerLock(name = "jwt-key-cleanup")
    public void cleanupOldKeys() {

        Instant now = Instant.now();
        long retentionSeconds =
                props.getKeyRotation().getRetainOldKeysHours() * 3600L;

        String activeKid = keyStore.getActiveKid();

        keyStore.getAllKeys().forEach((kid, stored) -> {
            boolean expired =
                    stored.createdAt()
                            .plusSeconds(retentionSeconds)
                            .isBefore(now);

            if (expired && !kid.equals(activeKid)) {
                keyStore.deleteKey(kid);
            }
        });
    }
}
