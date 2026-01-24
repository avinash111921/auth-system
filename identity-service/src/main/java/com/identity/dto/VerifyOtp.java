package com.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtp {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otp;
}
