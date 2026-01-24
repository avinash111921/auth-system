package com.password.security.grpc;

import com.password.security.service.PasswordService;
import com.security.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class SecurityGrpcService extends SecurityServiceGrpc.SecurityServiceImplBase {

    private final PasswordService passwordService;

    @Override
    public void passwordEncode(
            EncodeRequest request,
            StreamObserver<EncodeResponse> responseObserver) {

        String encodedPassword = passwordService.encode(request.getPassword());

        EncodeResponse response = EncodeResponse.newBuilder()
                .setEncodedPassword(encodedPassword)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void passwordVerify(
            VerifyRequest request,
            StreamObserver<VerifyResponse> responseObserver) {

        boolean isMatch = passwordService.verify(
                request.getPassword(),
                request.getEncodedPassword()
        );

        VerifyResponse response = VerifyResponse.newBuilder()
                .setIsMatch(isMatch)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
