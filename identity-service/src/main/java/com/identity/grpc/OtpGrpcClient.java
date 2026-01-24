package com.identity.grpc;

import com.otp.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class OtpGrpcClient {

    @GrpcClient("otp")
    private OtpServiceGrpc.OtpServiceBlockingStub otpStub;

    public boolean verifyOtp(String email, String otp) {
        OtpVerifyResponse response = otpStub.verifyOtp(
                OtpVerifyRequest.newBuilder()
                        .setEmail(email)
                        .setOtp(otp)
                        .build()
        );
        return response.getValid();
    }

    public boolean verifyPasswordResetOtp(String email, String otp) {
        OtpVerifyResponse response = otpStub.verifyPasswordResetOtp(
                OtpVerifyRequest.newBuilder()
                        .setEmail(email)
                        .setOtp(otp)
                        .build()
        );
        return response.getValid();
    }
}
