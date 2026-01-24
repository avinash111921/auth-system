package com.risk.util;

import com.risk.grpc.RiskLevel;

public class RiskCalculator {

    public static RiskLevel level(int score) {
        if (score <= 30) return RiskLevel.LOW;
        if (score <= 70) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }
}
