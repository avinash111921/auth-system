package com.identity.util;


import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public  class ChallengeIdGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
