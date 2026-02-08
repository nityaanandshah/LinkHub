package com.linkhub.common.util;

import java.security.SecureRandom;

/**
 * Base62 encoder for generating short URL keys.
 * Alphabet: 0-9a-zA-Z (62 characters)
 * 7-character keys = 62^7 = ~3.5 trillion unique URLs
 */
public final class Base62 {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Base62() {
        // Utility class — no instantiation
    }

    /**
     * Generate a random Base62 string of the given length.
     */
    public static String generateRandomKey(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(BASE)));
        }
        return sb.toString();
    }

    /**
     * Encode a long value to Base62 string.
     */
    public static String encode(long value) {
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long v = Math.abs(value);
        while (v > 0) {
            sb.append(ALPHABET.charAt((int) (v % BASE)));
            v /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to a long value.
     */
    public static long decode(String encoded) {
        long result = 0;
        for (int i = 0; i < encoded.length(); i++) {
            result = result * BASE + ALPHABET.indexOf(encoded.charAt(i));
        }
        return result;
    }
}
