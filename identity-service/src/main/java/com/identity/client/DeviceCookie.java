package com.identity.client;

import com.identity.util.Hmac;
import com.identity.util.RandomIds;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Component
public class DeviceCookie {

    static final String NAME = "__Host-device";
    private static final Duration MAX_AGE = Duration.ofDays(365);

    private final SecretKeySpec key;

    public DeviceCookie(@Value("${auth.device-cookie.secret}") String secret) {
        this.key = Hmac.key(secret, "auth.device-cookie");
    }

    /**
        The device ID from a cookie we issued, or empty if it is missing
        or was tampered with.
     */
    public Optional<String> read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, NAME);
        if (cookie == null) {
            return Optional.empty();
        }
        String value = cookie.getValue();
        int dot = value.lastIndexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        String deviceId = value.substring(0, dot);
        byte[] presented = decode(value.substring(dot + 1));
        return MessageDigest.isEqual(presented, Hmac.sign(key, deviceId)) ? Optional.of(deviceId) : Optional.empty();
    }

    public String issue(HttpServletResponse response) {
        String deviceId = RandomIds.next();
        ResponseCookie cookie = ResponseCookie.from(NAME, deviceId + "." + encode(Hmac.sign(key, deviceId)))
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return deviceId;
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
