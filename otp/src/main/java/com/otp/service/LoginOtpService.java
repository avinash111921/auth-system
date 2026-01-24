package com.otp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginOtpService {
    private final OtpService otpService;

    public void sendLoginOtp(String email,String challengeId){
        otpService.generateAndSendLoginOtp(email,challengeId);
    }

    public boolean verifyLoginOtp(String otp,String challengeId) {
        String key = otpService.buildLoginKey(challengeId);
        return otpService.verifyOtp(key, otp);
    }

}
