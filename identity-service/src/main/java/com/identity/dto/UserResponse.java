package com.identity.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    @NotBlank
    private String id;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    private boolean verified;

    @NotBlank
    private boolean enabled;
}