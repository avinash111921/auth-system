package com.identity.dto;

import lombok.Data;

@Data
public class OtpLoginRequest {
    private String email;
    private String otp;
    private String challengeId;
}
