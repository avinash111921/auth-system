package com.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueTokenRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String deviceFingerprint;

    @NotBlank
    private String ip;
}
