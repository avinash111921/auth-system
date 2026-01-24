package com.otp.service;

import com.otp.dto.OtpType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetOtpService {

    private final OtpService otpService;

    public void sendPasswordResetOtp(String email) {
        otpService.generateAndSendOtp(email, OtpType.PASSWORD_RESET);
    }

    public boolean verifyPasswordResetOtp(String email, String otp) {
        String key = otpService.buildKey(email,OtpType.PASSWORD_RESET);
        return otpService.verifyOtp(key, otp);
    }
}
