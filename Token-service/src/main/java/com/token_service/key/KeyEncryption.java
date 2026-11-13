package com.token_service.key;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
    AES-256-GCM for private signing keys at rest: a Redis dump or replica
    no longer hands out the keys that sign every access token.
    Values written before this existed are plain JSON; they are still
    readable and rotate out within the 24-hour retention window.
 */

@Component
public class KeyEncryption {

    private static final String VERSION = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] AAD = "auth-platform/jwt-signing-key".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public KeyEncryption(@Value("${jwt.key-encryption-secret}") String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("jwt.key-encryption-secret must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(sha256(raw), "AES");
    }

    public String encrypt(String plaintext) {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        byte[] sealed = cipher(Cipher.ENCRYPT_MODE, iv, plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] out = ByteBuffer.allocate(iv.length + sealed.length).put(iv).put(sealed).array();
        return VERSION + Base64.getEncoder().encodeToString(out);
    }

    public String decrypt(String stored) {
        if (!stored.startsWith(VERSION)) {
            return stored;
        }
        byte[] in = Base64.getDecoder().decode(stored.substring(VERSION.length()));
        byte[] iv = java.util.Arrays.copyOfRange(in, 0, IV_BYTES);
        byte[] sealed = java.util.Arrays.copyOfRange(in, IV_BYTES, in.length);
        return new String(cipher(Cipher.DECRYPT_MODE, iv, sealed), StandardCharsets.UTF_8);
    }

    private byte[] cipher(int mode, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(AAD);
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signing key could not be " + (mode == Cipher.ENCRYPT_MODE ? "sealed" : "opened"), e);
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
