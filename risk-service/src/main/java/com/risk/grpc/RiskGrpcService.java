package com.risk.grpc;

import com.risk.entity.RiskResult;
import com.risk.service.RiskEngine;
import com.risk.util.RiskCalculator;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class RiskGrpcService extends RiskServiceGrpc.RiskServiceImplBase {

    private final RiskEngine engine;

    @Override
    public void assessRisk(RiskRequest req,
                           StreamObserver<RiskResponse> res) {

        RiskResult result = engine.calculateRisk(
                req.getUserId(),
                req.getDeviceFingerprint(),
                req.getIp()
        );

        RiskLevel level = RiskCalculator.level(result.getScore());

        RiskResponse response = RiskResponse.newBuilder()
                .setScore(result.getScore())
                .setLevel(level)
                .addAllReasons(result.getReasons())
                .build();

        res.onNext(response);
        res.onCompleted();
    }

    @Override
    public void recordFailure(FailureRequest req,
                              StreamObserver<Empty> res) {

        engine.recordFailure(req.getUserId());

        res.onNext(Empty.getDefaultInstance());
        res.onCompleted();
    }

    @Override
    public void onLoginSuccess(LoginSuccessRequest req,
                               StreamObserver<Empty> res) {

        engine.onSuccessfulLogin(
                req.getUserId(),
                req.getDeviceFingerprint(),
                req.getIp()
        );

        res.onNext(Empty.getDefaultInstance());
        res.onCompleted();
    }
}
