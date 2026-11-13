package com.identity.service;

import com.identity.grpc.TokenPair;

public sealed interface LoginResult {

    record Authenticated(TokenPair tokens) implements LoginResult {
    }

    record ChallengeRequired(String challengeId) implements LoginResult {
    }
}
