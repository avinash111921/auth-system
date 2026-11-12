package com.otp.service;

import com.otp.grpc.Purpose;

public enum OtpPurpose {
    REGISTRATION("Confirm your email address"),
    PASSWORD_RESET("Reset your password"),
    LOGIN("Your sign-in code");

    private final String emailSubject;

    OtpPurpose(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String emailSubject() {
        return emailSubject;
    }

    public static OtpPurpose from(Purpose purpose) {
        return switch (purpose) {
            case PURPOSE_REGISTRATION -> REGISTRATION;
            case PURPOSE_PASSWORD_RESET -> PASSWORD_RESET;
            case PURPOSE_LOGIN -> LOGIN;
            default -> throw new IllegalArgumentException("unsupported OTP purpose");
        };
    }
}
