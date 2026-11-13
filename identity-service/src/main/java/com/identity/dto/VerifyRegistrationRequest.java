package com.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRegistrationRequest(
        @NotBlank String registrationId,
        @NotBlank @Pattern(regexp = "\\d{6}") String otp) {
}
