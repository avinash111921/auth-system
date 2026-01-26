package com.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RevokeTokenRequest {
    private String refreshToken;
    private String deviceHash;
}
