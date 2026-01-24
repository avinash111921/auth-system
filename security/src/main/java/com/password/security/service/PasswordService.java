package com.password.security.service;

public interface PasswordService {

    String encode(String rawPassword);

    boolean verify(String rawPassword, String encodedPassword);
}
