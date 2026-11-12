package com.otp.grpc;

import com.otp.service.OtpPurpose;
import com.otp.service.OtpService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

/** Only identity-service may call this (service-auth interceptor); failures map in {@link GrpcErrorAdvice}. */
@GrpcService
@RequiredArgsConstructor
public class OtpGrpcService extends OtpServiceGrpc.OtpServiceImplBase {

    private final OtpService otpService;

    @Override
    public void verify(VerifyRequest request, StreamObserver<VerifyResponse> out) {
        boolean valid = otpService.verify(OtpPurpose.from(request.getPurpose()), request.getSubject(), request.getCode());
        out.onNext(VerifyResponse.newBuilder().setValid(valid).build());
        out.onCompleted();
    }
}
