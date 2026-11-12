package com.risk.grpc;

import com.risk.entity.RiskResult;
import com.risk.service.RiskEngine;
import com.risk.util.RiskCalculator;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

/** Only identity-service may call this (service-auth interceptor); failures map in {@link GrpcErrorAdvice}. */
@GrpcService
@RequiredArgsConstructor
public class RiskGrpcService extends RiskServiceGrpc.RiskServiceImplBase {

    private final RiskEngine engine;

    @Override
    public void assessLogin(AssessLoginRequest request, StreamObserver<AssessLoginResponse> out) {
        if (request.getUserId().isBlank()) {
            throw new IllegalArgumentException("user_id is required");
        }
        RiskResult result = engine.calculateRisk(request.getUserId(), request.getDeviceId(), request.getIp());
        out.onNext(AssessLoginResponse.newBuilder()
                .setLevel(RiskCalculator.level(result.getScore()))
                .setScore(result.getScore())
                .addAllReasons(result.getReasons())
                .build());
        out.onCompleted();
    }
}
