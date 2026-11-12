package com.otp;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.TimeZone;

@SpringBootTest
class EmailConnectionTest {
    @BeforeAll
    static void setupTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }
    @Autowired
    private JavaMailSender mailSender;

    @Test
    void testEmailConnection() {
        SimpleMailMessage message = new SimpleMailMessage();
        // Replace with an email address you have access to
        message.setTo("apexavinash826@gmail.com");
        message.setSubject("SMTP Connection Test");
        message.setText("If you are reading this, your Spring Boot JavaMailSender is working!");

        // If this line fails, the test will throw an exception and fail
        mailSender.send(message);

        System.out.println("Test email dispatched successfully!");
    }
}