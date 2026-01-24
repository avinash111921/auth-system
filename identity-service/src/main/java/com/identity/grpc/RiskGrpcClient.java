package com.identity.grpc;

import com.risk.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class RiskGrpcClient {

    @GrpcClient("risk-service")
    private RiskServiceGrpc.RiskServiceBlockingStub stub;

    public RiskResponse assessRisk(String userId, String fp, String ip) {
        return stub.assessRisk(
                RiskRequest.newBuilder()
                        .setUserId(userId)
                        .setDeviceFingerprint(fp)
                        .setIp(ip)
                        .build()
        );
    }

    public void recordFailure(String userId) {
        stub.recordFailure(
                FailureRequest.newBuilder()
                        .setUserId(userId)
                        .build()
        );
    }

    public void trustContext(String userId, String fp, String ip) {
        stub.trustContext(
                TrustRequest.newBuilder()
                        .setUserId(userId)
                        .setDeviceFingerprint(fp)
                        .setIp(ip)
                        .build()
        );
    }
}
