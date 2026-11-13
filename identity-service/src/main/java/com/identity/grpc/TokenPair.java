package com.identity.grpc;

public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
}
