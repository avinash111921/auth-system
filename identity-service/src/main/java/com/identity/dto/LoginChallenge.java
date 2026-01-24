package com.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginChallenge {
    private String challengeId;
    private String userId;
    private String deviceFingerprint;
    private String ip;
    private String status;
}
