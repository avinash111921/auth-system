package com.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RevokeAllTokenRequest {
    private String userId;
}
