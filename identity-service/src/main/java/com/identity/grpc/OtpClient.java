package com.identity.grpc;

import com.otp.grpc.OtpServiceGrpc;
import com.otp.grpc.Purpose;
import com.otp.grpc.VerifyRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OtpClient {

    private static final long DEADLINE_MS = 2_000;

    @GrpcClient("otp")
    private OtpServiceGrpc.OtpServiceBlockingStub stub;

    public boolean verify(Purpose purpose, String subject, String code) {
        return stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS)
                .verify(VerifyRequest.newBuilder()
                        .setPurpose(purpose)
                        .setSubject(subject)
                        .setCode(code)
                        .build())
                .getValid();
    }
}
