package com.token_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccessTokenResult {
    private String token;
    private String jti;
}