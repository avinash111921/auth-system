package com.identity.service;

import com.identity.entity.User;
import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import com.identity.grpc.OtpClient;
import com.identity.outbox.AuthEvent.AccountExistsNotice;
import com.identity.outbox.AuthEvent.VerificationRequested;
import com.identity.outbox.OutboxWriter;
import com.identity.repo.UserRepository;
import com.identity.util.Emails;
import com.identity.util.RandomIds;
import com.otp.grpc.Purpose;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * A sign-up is a Redis entry keyed by a random registration ID until its OTP is verified. The
 * OTP is bound to that ID, so the password that ends up on the account is always the one
 * submitted by whoever can read the inbox; nobody can overwrite it by registering again.
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository users;
    private final PendingRegistrationStore pending;
    private final PasswordEncoder passwordEncoder;
    private final OtpClient otp;
    private final OutboxWriter outbox;

    /** Same response whether the email is taken, so registration can't enumerate accounts. */
    public String start(String rawEmail, String fullName, String password) {
        String email = Emails.normalize(rawEmail);
        String registrationId = RandomIds.next();
        // Hashed on both paths: skipping bcrypt for a taken email made that answer measurably faster.
        String passwordHash = passwordEncoder.encode(password);

        if (users.existsByEmail(email)) {
            outbox.append(new AccountExistsNotice(email));
            return registrationId;
        }
        pending.save(registrationId, new PendingRegistration(email, fullName.strip(), passwordHash));
        outbox.append(new VerificationRequested(registrationId, email));
        return registrationId;
    }

    public void complete(String registrationId, String code) {
        if (!otp.verify(Purpose.PURPOSE_REGISTRATION, registrationId, code)) {
            throw new AuthException(ErrorCode.INVALID_OTP);
        }
        PendingRegistration registration = pending.take(registrationId)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_OTP));
        try {
            users.saveAndFlush(User.verified(registration.email(), registration.fullName(), registration.passwordHash()));
        } catch (DataIntegrityViolationException alreadyRegistered) {
            throw new AuthException(ErrorCode.ACCOUNT_EXISTS);
        }
    }
}
