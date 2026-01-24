package com.otp.grpc;


import com.otp.service.OtpService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class OtpGrpcService extends OtpServiceGrpc.OtpServiceImplBase {

    private final OtpService otpService;

    @Override
    public void verifyOtp(
            OtpVerifyRequest request,
            StreamObserver<OtpVerifyResponse> responseObserver
    ) {

        try {
            boolean valid = otpService.verifyOtp(
                    request.getEmail(),
                    request.getOtp()
            );

            responseObserver.onNext(
                    OtpVerifyResponse.newBuilder()
                            .setValid(valid)
                            .build()
            );
            responseObserver.onCompleted();

        } catch (IllegalStateException ex) {
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription(ex.getMessage())
                            .asRuntimeException()
            );
        }
    }
    @Override
    public void verifyPasswordResetOtp(
            OtpVerifyRequest request,
            StreamObserver<OtpVerifyResponse> responseObserver) {

        boolean valid = otpService.verifyPasswordResetOtp(
                request.getEmail(),
                request.getOtp()
        );

        responseObserver.onNext(
                OtpVerifyResponse.newBuilder()
                        .setValid(valid)
                        .build()
        );
        responseObserver.onCompleted();
    }
}
