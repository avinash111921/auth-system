package com.identity.grpc;

import com.security.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class SecurityGrpcClient {

    @GrpcClient("security")
    private SecurityServiceGrpc.SecurityServiceBlockingStub securityStub;

    public String encode(String rawPassword) {
        EncodeResponse response = securityStub.passwordEncode(
                EncodeRequest
                        .newBuilder()
                        .setPassword(rawPassword)
                        .build()
        );
        return response.getEncodedPassword();
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        VerifyResponse response = securityStub.passwordVerify(
                VerifyRequest.newBuilder()
                        .setPassword(rawPassword)
                        .setEncodedPassword(encodedPassword)
                        .build()
        );
        return response.getIsMatch();
    }
}
