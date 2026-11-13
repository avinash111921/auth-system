package com.identity.grpc;

public record RiskDecision(Outcome outcome, int score) {

    public enum Outcome { ALLOW, CHALLENGE, DENY }
}
