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

    private static final Duration MAX_AGE = Duration.ofDays(365);

    private final SecretKeySpec key;
    private final String name;
    private final boolean secure;

    /**
     Defaults to a __Host- cookie (Secure, no Domain, Path=/).
     A local test client on plain HTTP in a browser that won't store
     Secure cookies for localhost can switch to
     name=device, secure=false; a __Host- name without Secure is refused at startup.
     */
    public DeviceCookie(@Value("${auth.device-cookie.secret}") String secret,
                        @Value("${auth.device-cookie.name:__Host-device}") String name,
                        @Value("${auth.device-cookie.secure:true}") boolean secure) {
        if (name.startsWith("__Host-") && !secure) {
            throw new IllegalStateException("auth.device-cookie.secure must be true for a __Host- cookie");
        }
        this.key = Hmac.key(secret, "auth.device-cookie");
        this.name = name;
        this.secure = secure;
    }

    /**
     The device ID from a cookie we issued, or empty if it is missing
     or was tampered with.
     */
    public Optional<String> read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, name);
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
        ResponseCookie cookie = ResponseCookie.from(name, deviceId + "." + encode(Hmac.sign(key, deviceId)))
                .httpOnly(true)
                .secure(secure)
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
