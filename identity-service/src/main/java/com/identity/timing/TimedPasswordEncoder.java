package com.identity.timing;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
    Reports password hashing as "bcrypt";
    at cost 10 it is usually the largest slice of a login.
 */
public class TimedPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegate;

    public TimedPasswordEncoder(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return ServerTiming.time("bcrypt", () -> delegate.encode(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return ServerTiming.time("bcrypt", () -> delegate.matches(rawPassword, encodedPassword));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }
}
