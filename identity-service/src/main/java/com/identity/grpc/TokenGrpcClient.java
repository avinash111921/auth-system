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
        if (deviceHash == null || deviceHash.isBlank()) {
            throw new IllegalArgumentException("Device hash missing");
        }

        IssueTokenRequest request = IssueTokenRequest.newBuilder()
                .setUserId(userId)
                .setDeviceHash(deviceHash)
                .build();

        return tokenStub.issueToken(request);
    }

    public IssueTokenResponse refreshToken(
            String refreshToken,
            String deviceHash,
            String accessJti
    ) {
        System.out.println("refreshToken"+refreshToken);
        System.out.println("device"+deviceHash);
        System.out.println("accessJti"+accessJti);

        if (deviceHash == null || deviceHash.isBlank()) {
            throw new IllegalArgumentException("Device hash missing");
        }
        RefreshTokenRequest request = RefreshTokenRequest.newBuilder()
                .setRefreshToken(refreshToken)
                .setDeviceHash(deviceHash)
                .setAccessJti(accessJti)
                .build();

        return tokenStub.refreshToken(request);
    }

    public RevokeResponse revokeToken (String refreshToken,String deviceHash) {
         if (deviceHash == null || deviceHash.isBlank()) {
             throw new IllegalArgumentException("Device hash missing");
         }

         RevokeTokenRequest request = RevokeTokenRequest.newBuilder()
                .setRefreshToken(refreshToken)
                .setDeviceHash(deviceHash)
                .build();

        return tokenStub.revokeToken(request);
     }
     public RevokeResponse revokeAllToken(String userId) {
        RevokeAllTokensRequest request = RevokeAllTokensRequest.newBuilder()
                .setUserId(userId)
                .build();

        return tokenStub.revokeAllTokens(request);
     }
}
