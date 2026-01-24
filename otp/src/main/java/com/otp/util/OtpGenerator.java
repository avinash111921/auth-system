package com.otp.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {
    public String generate() {
        return String.valueOf(
                100000 + new SecureRandom().nextInt(900000)
        );
    }
}