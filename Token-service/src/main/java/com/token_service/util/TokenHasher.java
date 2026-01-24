package com.token_service.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class TokenHasher {

    public String hash(String token) {
        return DigestUtils.sha256Hex(token);
    }
}
