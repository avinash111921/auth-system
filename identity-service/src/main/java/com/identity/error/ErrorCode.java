package com.identity.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_OTP(HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
    ACCOUNT_UNAVAILABLE(HttpStatus.FORBIDDEN),
    LOGIN_BLOCKED(HttpStatus.FORBIDDEN),
    ACCOUNT_EXISTS(HttpStatus.CONFLICT),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS),
    TEMPORARILY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
