package com.globalisosecurity.backend.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 6238 TOTP (HMAC-SHA1, six digits, 30-second period). */
public final class Totp {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final long PERIOD_MILLIS = 30_000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() {}

    public static String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public static String codeFor(String secret, long timeMillis) {
        long step = Math.floorDiv(timeMillis, PERIOD_MILLIS);
        if (step < 0) throw new IllegalArgumentException("El instante TOTP no puede ser negativo");
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secret), "HmacSHA1"));
            byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
            int offset = digest[digest.length - 1] & 0x0f;
            int binary = ((digest[offset] & 0x7f) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No fue posible calcular el código TOTP", ex);
        }
    }

    /** Accepts the current 30-second step and one adjacent step, but never replays a used step. */
    public static OptionalLong matchingStep(String secret, String code, long timeMillis, Long lastUsedStep) {
        if (code == null || !code.matches("[0-9]{6}")) return OptionalLong.empty();
        long currentStep = Math.floorDiv(timeMillis, PERIOD_MILLIS);
        for (long step = currentStep - 1; step <= currentStep + 1; step++) {
            if (step < 0 || (lastUsedStep != null && step <= lastUsedStep)) continue;
            byte[] expected = codeFor(secret, step * PERIOD_MILLIS).getBytes(StandardCharsets.US_ASCII);
            byte[] supplied = code.getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(expected, supplied)) return OptionalLong.of(step);
        }
        return OptionalLong.empty();
    }

    public static String base32Encode(byte[] bytes) {
        StringBuilder output = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                output.append(BASE32.charAt((buffer >>> (bitsLeft - 5)) & 0x1f));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) output.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 0x1f));
        return output.toString();
    }

    private static byte[] base32Decode(String value) {
        String normalized = value == null ? "" : value.replace("=", "").trim().toUpperCase(Locale.ROOT);
        byte[] output = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char character : normalized.toCharArray()) {
            int digit = BASE32.indexOf(character);
            if (digit < 0) throw new IllegalArgumentException("Secreto Base32 inválido");
            buffer = (buffer << 5) | digit;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[index++] = (byte) ((buffer >>> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return output;
    }
}
