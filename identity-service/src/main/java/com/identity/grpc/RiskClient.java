package com.identity.grpc;

import com.identity.client.ClientContext;
import com.risk.grpc.AssessLoginRequest;
import com.risk.grpc.RiskServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.identity.grpc.RiskDecision.Outcome.ALLOW;
import static com.identity.grpc.RiskDecision.Outcome.CHALLENGE;
import static com.identity.grpc.RiskDecision.Outcome.DENY;

/**
 * Fails safe in both directions: an unknown level is treated as high risk, and an unreachable
 * risk-service steps the user up to an OTP instead of either blocking logins or waving them through.
 */
@Component
@Slf4j
public class RiskClient {

    private static final long DEADLINE_MS = 300;

    @GrpcClient("risk-service")
    private RiskServiceGrpc.RiskServiceBlockingStub stub;

    public RiskDecision assess(UUID userId, ClientContext client) {
        try {
            var response = stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS)
                    .assessLogin(AssessLoginRequest.newBuilder()
                            .setUserId(userId.toString())
                            .setDeviceId(client.deviceId())
                            .setIp(client.ip())
                            .build());
            var outcome = switch (response.getLevel()) {
                case RISK_LEVEL_LOW -> ALLOW;
                case RISK_LEVEL_MEDIUM -> CHALLENGE;
                default -> DENY;
            };
            return new RiskDecision(outcome, response.getScore());
        } catch (StatusRuntimeException e) {
            log.warn("risk assessment failed with {}, stepping up to OTP", e.getStatus().getCode());
            return new RiskDecision(CHALLENGE, -1);
        }
    }
}
