package com.identity.controller;


import com.identity.client.ClientContext;
import com.identity.dto.*;
import com.identity.service.LoginService;
import com.identity.service.PasswordResetService;
import com.identity.service.RegistrationService;
import com.identity.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registration;
    private final LoginService login;
    private final PasswordResetService passwordReset;
    private final SessionService sessions;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegistrationStarted register(@Valid @RequestBody RegisterRequest body) {
        return new RegistrationStarted(registration.start(body.email(), body.fullName(), body.password()));
    }

    @PostMapping("/verify-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest body) {
        registration.complete(body.registrationId(), body.otp());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body, ClientContext client) {
        return LoginResponse.from(login.login(body.email(), body.password(), client));
    }

    @PostMapping("/otp-login")
    public TokenResponse loginWithOtp(@Valid @RequestBody LoginOtpRequest body, ClientContext client) {
        return TokenResponse.from(login.verifyChallenge(body.challengeId(), body.otp(), client));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        passwordReset.request(body.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        passwordReset.reset(body.email(), body.otp(), body.newPassword());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest body, ClientContext client) {
        return TokenResponse.from(sessions.refresh(body.refreshToken(), client));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest body) {
        sessions.logout(body.refreshToken());
    }

    /** A protected resource for clients to check their access token against. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return MeResponse.from(sessions.currentUser(UUID.fromString(jwt.getSubject())), jwt);
    }

    /** The user comes from the verified access token; a request body can't name someone else. */
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal Jwt jwt) {
        sessions.logoutEverywhere(UUID.fromString(jwt.getSubject()));
    }
}
