package com.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenRefreshRequest {
    private String refreshToken;
    private String deviceHash;
    private String accessJti;
}
