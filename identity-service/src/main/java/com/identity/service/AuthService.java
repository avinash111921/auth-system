package com.identity.service;

import com.identity.dto.*;
import com.identity.entity.User;
import com.identity.grpc.RiskGrpcClient;
import com.identity.grpc.SecurityGrpcClient;
import com.identity.kafka.AuthEventProducer;
import com.identity.repo.UserRepository;
import com.identity.grpc.OtpGrpcClient;
import com.identity.grpc.TokenGrpcClient;
import com.identity.util.ChallengeIdGenerator;
import com.identity.util.LoginChallengeStore;
import com.risk.grpc.RiskLevel;
import com.risk.grpc.RiskResponse;
import com.token_service.grpc.IssueTokenResponse;
import com.token_service.grpc.RevokeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RiskGrpcClient riskGrpcClient;
    private final OtpGrpcClient otpGrpcClient;
    private final TokenGrpcClient tokenGrpcClient;
    private final SecurityGrpcClient securityGrpcClient;
    private final AuthEventProducer eventProducer;
    private final ChallengeIdGenerator challengeIdGenerator;
    private final LoginChallengeStore loginChallengeStore;

    public UserResponse register(RegisterRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("email_already_exists");
        }

        String passwordHash = securityGrpcClient.encode(req.getPassword());

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordHash)
                .fullName(req.getFullName())
                .verified(false)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        User finalUser = user;

        eventProducer.publish(
                "USER_REGISTERED", Map.of(
                "userId", finalUser.getId(),
                "email", finalUser.getEmail()
        ));

        return UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .fullName(user.getFullName())
                .verified(user.isVerified())
                .enabled(user.isEnabled())
                .build();
    }

    public void verifyOtp(String email, String otp) {

        Optional<User> opt = userRepository.findByEmail(email);

        if (opt.isEmpty()) {
            throw new IllegalArgumentException("user_not_found");
        }

        boolean ok = otpGrpcClient.verifyOtp(email, otp);

        if (!ok) {
            throw new IllegalArgumentException("invalid_otp");
        }

        User user = opt.get();
        user.setVerified(true);

        userRepository.save(user);
    }

    public Object login(LoginRequest req) {

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("invalid_credentials"));

        if (!user.isEnabled()) throw new IllegalStateException("account_disabled");
        if (!user.isVerified()) throw new IllegalStateException("not_verified");
        if (user.isLocked()) throw new IllegalStateException("account_locked");

        boolean matches = securityGrpcClient.verifyPassword(req.getPassword(), user.getPasswordHash());

        if (!matches) {
            eventProducer.publish("LOGIN_FAILED", Map.of(
                    "userId", user.getId(),
                    "ip", req.getIp(),
                    "deviceFingerprint", req.getDeviceFingerprint()
            ));
            throw new IllegalArgumentException("invalid_credentials");
        }

        RiskResponse risk = riskGrpcClient.assessRisk(
                user.getId().toString(),
                req.getDeviceFingerprint(),
                req.getIp()
        );

        if (risk.getLevel() == RiskLevel.HIGH) {
            user.setLocked(true);
            userRepository.save(user);
            eventProducer.publish("HIGH_RISK_LOGIN", Map.of(
                    "userId", user.getId(),
                    "ip", req.getIp(),
                    "score", risk.getScore()
            ));
            throw new IllegalStateException("high_risk_login_blocked");
        }

        if (risk.getLevel() == RiskLevel.MEDIUM) {
            String challengeId = challengeIdGenerator.generate();
            LoginChallenge challenge = new LoginChallenge(
                    challengeId,
                    user.getId().toString(),
                    req.getDeviceFingerprint(),
                    req.getIp(),
                    "OTP_PENDING"
            );
            loginChallengeStore.save(challenge);

            eventProducer.publish("LOGIN_CHALLENGE_REQUIRED", Map.of(
                    "challengeId", challengeId,
                    "userId", user.getId(),
                    "email", user.getEmail()
            ));
            return Map.of(
                    "status", "OTP_REQUIRED",
                    "challengeId", challengeId,
                    "message", "additional_verification_required"
            );
        }
        IssueTokenRequest request = new IssueTokenRequest(
                req.getEmail(),
                req.getDeviceFingerprint(),
                req.getIp()
        );
        return issueToken(user, request);
    }

    public TokenResponse verifyLoginOtp (String email, String otp, String challengeId) {
        Optional<User> opt = userRepository.findByEmail(email);

        if (opt.isEmpty()) {
            throw new IllegalArgumentException("user_not_found");
        }

        boolean ok = otpGrpcClient.verifyLoginOtp(email,challengeId,otp);

        if(!ok){
            throw new IllegalArgumentException("invalid_otp");
        }

        User user = opt.get();

        LoginChallenge challenge = loginChallengeStore.get(challengeId);

        if(challenge==null || !challenge.getUserId().equals(user.getId().toString())) {
            throw new IllegalArgumentException("invalid_challenge");
        }
        loginChallengeStore.delete(challengeId);
        return issueToken(user, new IssueTokenRequest(
                email,
                challenge.getDeviceFingerprint(),
                challenge.getIp()
        ));
    }
    private TokenResponse issueToken(User user,IssueTokenRequest req){
        var tokenResponse = tokenGrpcClient.issueToken(
                user.getId().toString(),
                req.getDeviceFingerprint()
        );

        riskGrpcClient.onLoginSuccess(
                user.getId().toString(),
                req.getDeviceFingerprint(),
                req.getIp()
        );

        user.setDeviceFingerprint(req.getDeviceFingerprint());
        user.setLastLoginIp(req.getIp());
        userRepository.save(user);

        eventProducer.publish("LOGIN_SUCCESS", Map.of(
                "userId", user.getId(),
                "ip", req.getIp(),
                "deviceFingerprint", req.getDeviceFingerprint()
        ));

        return new TokenResponse(
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getAccessJti(),
                tokenResponse.getExpiresIn()
        );
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        eventProducer.publish("PASSWORD_RESET_REQUESTED", new HashMap<String,Object>(){{
            put("userId", user.getId());
            put("email", user.getEmail());
        }});

    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        boolean ok = otpGrpcClient.verifyPasswordResetOtp(email, otp);

        if (!ok) throw new IllegalArgumentException("invalid_otp");

        String newHashPassword = securityGrpcClient.encode(newPassword);

        user.setPasswordHash(newHashPassword);
        userRepository.save(user);
    }

    public TokenResponse refresh(String refreshToken, String deviceHash,String accessJti) {
        IssueTokenResponse response = tokenGrpcClient.refreshToken(refreshToken, deviceHash,accessJti);
        return new TokenResponse(
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getAccessJti(),
                response.getExpiresIn()
        );
    }

    public boolean revokeToken(String refreshToken, String deviceHash) {
        RevokeResponse response = tokenGrpcClient.revokeToken(refreshToken, deviceHash);
        return response.getSuccess();
    }

    public boolean revokeAllTokens(String userId) {
        RevokeResponse response = tokenGrpcClient.revokeAllToken(userId);
        return response.getSuccess();
    }
}
