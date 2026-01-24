package com.identity.grpc;

import com.token_service.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class TokenGrpcClient {

    @GrpcClient("token-service")
    private TokenServiceGrpc.TokenServiceBlockingStub tokenStub;

    public IssueTokenResponse issueToken(
            String userId,
            String deviceHash
    ) {

        IssueTokenRequest request = IssueTokenRequest.newBuilder()
                .setUserId(userId)
                .setDeviceHash(deviceHash == null ? "" : deviceHash)
                .build();

        return tokenStub.issueToken(request);
    }

    public IssueTokenResponse refreshToken(
            String refreshToken,
            String deviceHash
    ) {

        RefreshTokenRequest request = RefreshTokenRequest.newBuilder()
                .setRefreshToken(refreshToken)
                .setDeviceHash(deviceHash == null ? "" : deviceHash)
                .build();

        return tokenStub.refreshToken(request);
    }
}
