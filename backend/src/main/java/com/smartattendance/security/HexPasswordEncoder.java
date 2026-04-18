package com.smartattendance.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class HexPasswordEncoder implements PasswordEncoder {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = deriveKey(rawPassword, salt);
        return toHex(salt) + toHex(hash);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        int expectedLength = (SALT_BYTES + HASH_BYTES) * 2;
        if (encodedPassword.length() != expectedLength) {
            return false;
        }

        byte[] salt = fromHex(encodedPassword.substring(0, SALT_BYTES * 2));
        byte[] expectedHash = fromHex(encodedPassword.substring(SALT_BYTES * 2));
        byte[] actualHash = deriveKey(rawPassword, salt);
        return constantTimeEquals(expectedHash, actualHash);
    }

    private byte[] deriveKey(CharSequence rawPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toString().toCharArray(), salt, ITERATIONS, HASH_BYTES * 8);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
                return factory.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encode password", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >>> 4) & 0x0F, 16));
            builder.append(Character.forDigit(value & 0x0F, 16));
        }
        return builder.toString();
    }

    private byte[] fromHex(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid hex password encoding");
            }
            result[i / 2] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left.length != right.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length; i++) {
            diff |= left[i] ^ right[i];
        }
        return diff == 0;
    }
}
