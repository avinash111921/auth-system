package com.identity.error;

/**
    An expected, client-caused failure.
    No stack trace: these fire on every wrong password.
 */

public class AuthException extends RuntimeException {

    private final ErrorCode code;

    public AuthException(ErrorCode code) {
        super(code.name(), null, false, false);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
