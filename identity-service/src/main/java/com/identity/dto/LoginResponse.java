package com.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.identity.service.LoginResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(Status status, TokenResponse tokens, String challengeId) {

    public enum Status { AUTHENTICATED, OTP_REQUIRED }

    public static LoginResponse from(LoginResult result) {
        return switch (result) {
            case LoginResult.Authenticated a -> new LoginResponse(Status.AUTHENTICATED, TokenResponse.from(a.tokens()), null);
            case LoginResult.ChallengeRequired c -> new LoginResponse(Status.OTP_REQUIRED, null, c.challengeId());
        };
    }
}
