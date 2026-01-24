package com.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenResponse {
    @NotBlank
    private String accessToken;

    @NotBlank
    private String refreshToken;

    @NotBlank
    private long expiresIn;
}