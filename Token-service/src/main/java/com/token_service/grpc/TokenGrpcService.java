package com.token_service.grpc;

import com.token_service.config.JwtProperties;
import com.token_service.entity.AccessTokenResult;
import com.token_service.service.RefreshTokenService;
import com.token_service.service.RevokeService;
import com.token_service.service.TokenService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@AllArgsConstructor
public class TokenGrpcService extends TokenServiceGrpc.TokenServiceImplBase {

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final RevokeService revokeService;

    @Override
    public void issueToken(
            IssueTokenRequest request,
            StreamObserver<IssueTokenResponse> responseObserver
    ) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            String deviceHash = request.getDeviceHash();

            AccessTokenResult accessToken =
                    tokenService.generateAccessToken(userId, deviceHash);

            String accessJti = accessToken.getJti();

            String refreshToken =
                    refreshTokenService.create(
                            userId,
                            deviceHash,
                            accessJti
                    );

            IssueTokenResponse response =
                    IssueTokenResponse.newBuilder()
                            .setAccessToken(accessToken.getToken())
                            .setRefreshToken(refreshToken)
                            .setAccessJti(accessJti)
                            .setExpiresIn(jwtProperties.getAccessTtl())
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception ex) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(ex.getMessage())
                            .withCause(ex)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void refreshToken(
            RefreshTokenRequest request,
            StreamObserver<IssueTokenResponse> responseObserver
    ) {
        try {
            UUID userId =
                    refreshTokenService.rotate(
                            request.getRefreshToken(),
                            request.getDeviceHash(),
                            request.getAccessJti()
                    );

            AccessTokenResult accessToken =
                    tokenService.generateAccessToken(
                            userId,
                            request.getDeviceHash()
                    );

            String newRefreshToken = refreshTokenService.create(
                    userId,
                    request.getDeviceHash(),
                    accessToken.getJti()
            );

            IssueTokenResponse response =
                    IssueTokenResponse.newBuilder()
                            .setAccessToken(accessToken.getToken())
                            .setRefreshToken(newRefreshToken)
                            .setAccessJti(accessToken.getJti())
                            .setExpiresIn(jwtProperties.getAccessTtl())
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException ex) {
            responseObserver.onError(
                    Status.UNAUTHENTICATED
                            .withDescription("Invalid or expired refresh token")
                            .withCause(ex)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void revokeToken(
            RevokeTokenRequest request,
            StreamObserver<RevokeResponse> responseObserver
    ) {
        try{
            revokeService.revokeSingle(request.getRefreshToken(),request.getDeviceHash());
            responseObserver.onNext(
                    RevokeResponse.newBuilder()
                            .setSuccess(true)
                            .build()
            );
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
        }
    }
    @Override
    public void revokeAllTokens(
            RevokeAllTokensRequest request,
            StreamObserver<RevokeResponse> responseObserver
    ) {
        try {
            revokeService.revokeAllForUser(
                    UUID.fromString(request.getUserId())
            );

            responseObserver.onNext(
                    RevokeResponse.newBuilder()
                            .setSuccess(true)
                            .build()
            );
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
        }
    }
}
