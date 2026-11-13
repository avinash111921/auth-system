package com.identity.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class RandomIds {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomIds() {
    }

    public static String next() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
