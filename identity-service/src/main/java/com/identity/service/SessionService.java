package com.identity.service;

import com.identity.client.ClientContext;
import com.identity.entity.User;
import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import com.identity.grpc.TokenClient;
import com.identity.grpc.TokenPair;
import com.identity.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final TokenClient tokens;
    private final UserRepository users;

    /**
        A disabled or deleted account is refused even while its access token is
        still valid.
     */

    public User currentUser(UUID userId) {
        return users.findById(userId)
                .filter(User::canLogIn)
                .orElseThrow(() -> new AuthException(ErrorCode.ACCOUNT_UNAVAILABLE));
    }

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
