package com.risk.util;

import com.risk.grpc.RiskLevel;

public final class RiskCalculator {

    private RiskCalculator() {
    }

    public static RiskLevel level(int score) {
        if (score <= 30) return RiskLevel.RISK_LEVEL_LOW;
        if (score <= 70) return RiskLevel.RISK_LEVEL_MEDIUM;
        return RiskLevel.RISK_LEVEL_HIGH;
    }
}
