package com.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgetPasswordRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    private String newPassword;
}