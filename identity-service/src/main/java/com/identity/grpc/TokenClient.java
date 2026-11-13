package com.identity.grpc;

import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import com.token_service.grpc.IssueTokenRequest;
import com.token_service.grpc.RefreshTokenRequest;
import com.token_service.grpc.RevokeAllTokensRequest;
import com.token_service.grpc.RevokeTokenRequest;
import com.token_service.grpc.TokenServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class TokenClient {

    private static final long DEADLINE_MS = 2_000;

    @GrpcClient("token-service")
    private TokenServiceGrpc.TokenServiceBlockingStub stub;

    public TokenPair issue(UUID userId, String deviceId) {
        var response = stub().issueToken(IssueTokenRequest.newBuilder()
                .setUserId(userId.toString())
                .setDeviceId(deviceId)
                .build());
        return new TokenPair(response.getAccessToken(), response.getRefreshToken(), response.getExpiresIn());
    }

    public TokenPair refresh(String refreshToken, String deviceId) {
        try {
            var response = stub().refreshToken(RefreshTokenRequest.newBuilder()
                    .setRefreshToken(refreshToken)
                    .setDeviceId(deviceId)
                    .build());
            return new TokenPair(response.getAccessToken(), response.getRefreshToken(), response.getExpiresIn());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
                throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            throw e;
        }
    }

    public void revoke(String refreshToken) {
        stub().revokeToken(RevokeTokenRequest.newBuilder().setRefreshToken(refreshToken).build());
    }

    public void revokeAll(UUID userId) {
        stub().revokeAllTokens(RevokeAllTokensRequest.newBuilder().setUserId(userId.toString()).build());
    }

    private TokenServiceGrpc.TokenServiceBlockingStub stub() {
        return stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS);
    }
}
