package com.otp.grpc;


import com.otp.service.LoginOtpService;
import com.otp.service.PasswordResetOtpService;
import com.otp.service.RegisterOtpService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;


@GrpcService
@RequiredArgsConstructor
public class OtpGrpcService extends OtpServiceGrpc.OtpServiceImplBase {

    private final PasswordResetOtpService passwordResetOtpService;
    private final RegisterOtpService registerOtpService;
    private final LoginOtpService loginOtpService;

    @Override
    public void verifyOtp(
            OtpVerifyRequest request,
            StreamObserver<OtpVerifyResponse> responseObserver
    ) {

        try {
            boolean valid = registerOtpService.verifyOtp(
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

        boolean valid = passwordResetOtpService.verifyPasswordResetOtp(
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

    @Override
    public void verifyLoginOtp(
            VerifyLoginOtpRequest request,
            StreamObserver<VerifyLoginOtpResponse> responseObserver
    ) {
      boolean valid = loginOtpService.verifyLoginOtp(
              request.getOtp(),
              request.getChallengeId()
      );

      responseObserver.onNext(
              VerifyLoginOtpResponse.newBuilder()
                      .setValid(valid)
                      .build()
      );
      responseObserver.onCompleted();
    }
}
