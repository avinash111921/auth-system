package com.token_service.controller;

import com.token_service.config.JwtProperties;
import com.token_service.entity.AccessTokenResult;
import com.token_service.entity.RefreshRotationResult;
import com.token_service.service.RevokeService;
import com.token_service.service.RefreshTokenService;
import com.token_service.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test/token")
@RequiredArgsConstructor
public class TokenTestController {

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final RevokeService revokeService;
    private final JwtProperties jwtProperties;

    @PostMapping("/issue")
    public ResponseEntity<?> issueToken(
            @RequestParam UUID userId,
            @RequestParam String deviceHash
    ) {
        AccessTokenResult access =
                tokenService.generateAccessToken(userId, deviceHash);

        String refresh =
                refreshTokenService.create(
                        userId,
                        deviceHash,
                        access.getJti()
                );

        return ResponseEntity.ok(
                Map.of(
                        "accessToken", access.getToken(),
                        "accessJti", access.getJti(),
                        "refreshToken", refresh,
                        "expiresIn", jwtProperties.getAccessTtl()
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestParam String refreshToken,
            @RequestParam String deviceHash,
            @RequestParam String accessJti
    ) {
        RefreshRotationResult result =
                refreshTokenService.rotate(
                        refreshToken,
                        deviceHash,
                        accessJti
                );

        UUID userId = result.getUserId();

        AccessTokenResult access =
                tokenService.generateAccessToken(userId, deviceHash);

        String newRefresh = result.getRefreshToken();

        return ResponseEntity.ok(
                Map.of(
                        "accessToken", access.getToken(),
                        "accessJti", access.getJti(),
                        "refreshToken", newRefresh,
                        "expiresIn", jwtProperties.getAccessTtl()
                )
        );
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokeSingle(
            @RequestParam String refreshToken,
            @RequestParam String deviceHash
    ) {
        revokeService.revokeSingle(refreshToken, deviceHash);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<?> revokeAll(
            @RequestParam UUID userId
    ) {
        revokeService.revokeAllForUser(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
