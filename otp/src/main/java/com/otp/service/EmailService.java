package com.otp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendCode(String to, OtpPurpose purpose, String code, Duration validFor) {
        send(to, purpose.emailSubject(), """
                Your code is %s. It expires in %d minutes.
                If you didn't ask for this, you can ignore this email.
                """.formatted(code,validFor.toMinutes()));
    }

    public void sendAccountExistsNotice(String to) {
        send(to,"You already have an account", """
                Someone tried to create an account with this email address, but one already exists.
                If it was you, sign in or reset your password instead. If not, you can ignore this email.
                """
        );
    }

    private void send(String to, String subject, String text){
        var message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
