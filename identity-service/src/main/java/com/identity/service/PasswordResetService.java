package com.identity.service;

import com.identity.entity.User;
import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import com.identity.grpc.OtpClient;
import com.identity.grpc.TokenClient;
import com.identity.outbox.AuthEvent.PasswordResetRequested;
import com.identity.outbox.OutboxWriter;
import com.identity.repo.UserRepository;
import com.identity.util.Emails;
import com.otp.grpc.Purpose;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OtpClient otp;
    private final TokenClient tokens;
    private final LoginThrottle throttle;
    private final OutboxWriter outbox;

    /** Silent when the email is unknown, so this endpoint can't be used to enumerate accounts. */
    public void request(String rawEmail) {
        String email = Emails.normalize(rawEmail);
        users.findByEmail(email).ifPresent(user -> outbox.append(new PasswordResetRequested(user.getId(), email)));
    }

    public void reset(String rawEmail, String code, String newPassword) {
        String email = Emails.normalize(rawEmail);
        if (!otp.verify(Purpose.PURPOSE_PASSWORD_RESET, email, code)) {
            throw new AuthException(ErrorCode.INVALID_OTP);
        }
        User user = users.findByEmail(email).orElseThrow(() -> new AuthException(ErrorCode.INVALID_OTP));
        String newHash = passwordEncoder.encode(newPassword);

        // Sessions go first: if the save below fails the user is merely signed out, whereas the
        // opposite order could leave an attacker's session alive on an account with a new password.
        tokens.revokeAll(user.getId());
        user.changePassword(newHash);
        users.save(user);
        throttle.reset(email);
    }
}
