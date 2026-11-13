package com.identity.service;

import com.identity.client.ClientContext;
import com.identity.grpc.TokenClient;
import com.identity.grpc.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final TokenClient tokens;

    public TokenPair refresh(String refreshToken, ClientContext client) {
        return tokens.refresh(refreshToken, client.deviceId());
    }

    public void logout(String refreshToken) {
        tokens.revoke(refreshToken);
    }

    public void logoutEverywhere(UUID userId) {
        tokens.revokeAll(userId);
    }
}
