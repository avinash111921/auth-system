package com.token_service.grpc;

import com.google.protobuf.Empty;
import com.token_service.config.JwtProperties;
import com.token_service.service.RefreshSessions;
import com.token_service.service.TokenService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

/** Straight-line handlers: failures are translated once, in {@link GrpcErrorAdvice}. */
@GrpcService
@RequiredArgsConstructor
public class TokenGrpcService extends TokenServiceGrpc.TokenServiceImplBase {

    private final TokenService accessTokens;
    private final RefreshSessions refreshSessions;
    private final JwtProperties props;

    @Override
    public void issueToken(IssueTokenRequest request, StreamObserver<TokenPair> out) {
        UUID userId = UUID.fromString(request.getUserId());
        String refreshToken = refreshSessions.start(userId, requireDeviceId(request.getDeviceId()));
        respond(out, tokenPair(userId, refreshToken));
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<TokenPair> out) {
        var rotation = refreshSessions.rotate(request.getRefreshToken(), requireDeviceId(request.getDeviceId()));
        respond(out, tokenPair(rotation.userId(), rotation.refreshToken()));
    }

    @Override
    public void revokeToken(RevokeTokenRequest request, StreamObserver<Empty> out) {
        refreshSessions.revoke(request.getRefreshToken());
        respond(out, Empty.getDefaultInstance());
    }

    @Override
    public void revokeAllTokens(RevokeAllTokensRequest request, StreamObserver<Empty> out) {
        refreshSessions.revokeAll(UUID.fromString(request.getUserId()));
        respond(out, Empty.getDefaultInstance());
    }

    private TokenPair tokenPair(UUID userId, String refreshToken) {
        return TokenPair.newBuilder()
                .setAccessToken(accessTokens.generateAccessToken(userId))
                .setRefreshToken(refreshToken)
                .setExpiresIn(props.getAccessTtl())
                .build();
    }

    private static String requireDeviceId(String deviceId) {
        if (deviceId.isBlank()) {
            throw new IllegalArgumentException("device_id is required");
        }
        return deviceId;
    }

    private static <T> void respond(StreamObserver<T> out, T value) {
        out.onNext(value);
        out.onCompleted();
    }
}
