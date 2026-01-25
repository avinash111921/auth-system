package com.risk.grpc;

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

        int score = engine.calculateRisk(
                req.getUserId(),
                req.getDeviceFingerprint(),
                req.getIp()
        );

        RiskLevel level = RiskCalculator.level(score);

        res.onNext(
                RiskResponse.newBuilder()
                        .setScore(score)
                        .setLevel(level)
                        .build()
        );
        res.onCompleted();
    }

    @Override
    public void recordFailure(FailureRequest req,
                              StreamObserver<Empty> res) {

        engine.recordFailure(req.getUserId());

        res.onNext(Empty.newBuilder().build());
        res.onCompleted();
    }

    @Override
    public void trustContext(TrustRequest req,
                             StreamObserver<Empty> res) {

        engine.trustContext(
                req.getUserId(),
                req.getDeviceFingerprint(),
                req.getIp()
        );

        res.onNext(Empty.newBuilder().build());
        res.onCompleted();
    }
}
