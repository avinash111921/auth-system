package com.token_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RefreshRotationResult{
        private UUID userId;
        private String refreshToken;
}
