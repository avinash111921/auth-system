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
}