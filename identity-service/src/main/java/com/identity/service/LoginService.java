package com.identity.service;

import com.identity.client.ClientContext;
import com.identity.entity.User;
import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import com.identity.grpc.OtpClient;
import com.identity.grpc.RiskClient;
import com.identity.grpc.RiskDecision;
import com.identity.grpc.TokenClient;
import com.identity.grpc.TokenPair;
import com.identity.outbox.AuthEvent.LoginBlocked;
import com.identity.outbox.AuthEvent.LoginChallengeRequired;
import com.identity.outbox.AuthEvent.LoginFailed;
import com.identity.outbox.AuthEvent.LoginSucceeded;
import com.identity.outbox.OutboxWriter;
import com.identity.repo.UserRepository;
import com.identity.util.Emails;
import com.identity.util.RandomIds;
import com.otp.grpc.Purpose;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LoginThrottle throttle;
    private final OtpGuessBudget otpGuesses;
    private final LoginChallengeStore challenges;
    private final RiskClient risk;
    private final OtpClient otp;
    private final TokenClient tokens;
    private final OutboxWriter outbox;
    private final String unknownUserHash;

    public LoginService(UserRepository users, PasswordEncoder passwordEncoder, LoginThrottle throttle,
                        OtpGuessBudget otpGuesses, LoginChallengeStore challenges, RiskClient risk, OtpClient otp, TokenClient tokens,
                        OutboxWriter outbox) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.throttle = throttle;
        this.otpGuesses = otpGuesses;
        this.challenges = challenges;
        this.risk = risk;
        this.otp = otp;
        this.tokens = tokens;
        this.outbox = outbox;
        this.unknownUserHash = passwordEncoder.encode(RandomIds.next());
    }

    public LoginResult login(String rawEmail, String password, ClientContext client) {
        String email = Emails.normalize(rawEmail);
        throttle.acquire(email, client.ip());

        Optional<User> found = users.findByEmail(email);
        // Hash even for unknown emails so response time doesn't reveal which accounts exist.
        boolean matches = passwordEncoder.matches(password, found.map(User::getPasswordHash).orElse(unknownUserHash));

        if (found.isEmpty() || !matches) {
            found.ifPresent(user -> outbox.append(new LoginFailed(user.getId(), client.ip(), client.deviceId())));
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (!user.canLogIn()) {
            throw new AuthException(ErrorCode.ACCOUNT_UNAVAILABLE);
        }
        throttle.succeeded(email, client.ip());

        RiskDecision decision = risk.assess(user.getId(), client);
        return switch (decision.outcome()) {
            case ALLOW -> new LoginResult.Authenticated(complete(user, client));
            case CHALLENGE -> challenge(user, client);
            case DENY -> {
                outbox.append(new LoginBlocked(user.getId(), client.ip(), client.deviceId(), decision.score()));
                throw new AuthException(ErrorCode.LOGIN_BLOCKED);
            }
        };
    }

    public TokenPair verifyChallenge(String challengeId, String code, ClientContext client) {
        LoginChallenge challenge = challenges.find(challengeId)
                .filter(c -> c.deviceId().equals(client.deviceId()))
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_OTP));

        String account = challenge.userId().toString();
        otpGuesses.spend("login", account);
        if (!otp.verify(Purpose.PURPOSE_LOGIN, challengeId, code) || challenges.take(challengeId).isEmpty()) {
            throw new AuthException(ErrorCode.INVALID_OTP);
        }
        otpGuesses.clear("login", account);

        User user = users.findById(challenge.userId())
                .filter(User::canLogIn)
                .orElseThrow(() -> new AuthException(ErrorCode.ACCOUNT_UNAVAILABLE));
        return complete(user, client);
    }

    private LoginResult challenge(User user, ClientContext client) {
        String challengeId = RandomIds.next();
        challenges.save(challengeId, new LoginChallenge(user.getId(), client.deviceId()));
        outbox.append(new LoginChallengeRequired(challengeId, user.getId(), user.getEmail()));
        return new LoginResult.ChallengeRequired(challengeId);
    }

    // Tokens first, event second: a failed append costs an unused token pair, whereas the reverse
    // order could tell risk-service to trust a device for a login that never happened.
    private TokenPair complete(User user, ClientContext client) {
        TokenPair pair = tokens.issue(user.getId(), client.deviceId());
        outbox.append(new LoginSucceeded(user.getId(), client.ip(), client.deviceId()));
        return pair;
    }
}
