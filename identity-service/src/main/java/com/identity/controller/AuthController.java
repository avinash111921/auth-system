package com.identity.controller;

import com.identity.dto.*;
import com.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        UserResponse user = authService.register(req);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtp req) {
        authService.verifyOtp(req.getEmail(), req.getOtp());
        return ResponseEntity.ok("verified");
    }

    @PostMapping("/otp-login")
    public ResponseEntity<?> otpLogin(@RequestBody OtpLoginRequest req) {
        TokenResponse response = authService.verifyLoginOtp(req.getEmail(), req.getOtp(), req.getChallengeId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody OtpRequestForgetPassword req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok("otp_sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ForgetPasswordRequest req) {
        authService.resetPassword(req.getEmail(), req.getOtp(), req.getNewPassword());
        return ResponseEntity.ok("password_changed");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest req) {
        TokenResponse tokenResponse = authService.refresh(req.getRefreshToken(), req.getDeviceHash(),req.getAccessJti());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody RevokeTokenRequest req
    ) {
        boolean response = authService.revokeToken(req.getRefreshToken(), req.getDeviceHash());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(
            @RequestBody RevokeAllTokenRequest req
    ) {
        boolean response = authService.revokeAllTokens(req.getUserId());
        return ResponseEntity.ok(response);
    }
}