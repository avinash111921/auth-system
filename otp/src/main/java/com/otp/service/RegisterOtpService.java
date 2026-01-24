package com.otp.service;

import com.otp.dto.OtpType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterOtpService {

    private final OtpService otpService;

    public void sendVerificationOtp(String email) {
        otpService.generateAndSendOtp(email, OtpType.VERIFICATION);
    }

    public boolean verifyOtp(String email, String otp) {
        String key = otpService.buildKey(email,OtpType.VERIFICATION);
        return otpService.verifyOtp(key, otp);
    }
}