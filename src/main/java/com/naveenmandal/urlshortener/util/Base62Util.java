package com.naveenmandal.urlshortener.util;

public class Base62Util {

    // Our character set: 0-9, a-z, A-Z  (62 total)
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    /**
     * Converts a long number to a Base62 string.
     * Example: encode(125) → "2b"
     *
     * Think of it like converting decimal to binary,
     * but instead of base 2, we use base 62.
     */
    public static String encode(long number) {
        if (number == 0) return "0";

        StringBuilder result = new StringBuilder();

        while (number > 0) {
            int remainder = (int) (number % BASE);   // get last digit in base62
            result.append(CHARS.charAt(remainder));   // map digit to character
            number /= BASE;                           // move to next digit
        }

        return result.reverse().toString(); // we built it backwards, so reverse
    }

    /**
     * Converts a Base62 string back to a long number.
     * Example: decode("2b") → 125
     */
    public static long decode(String code) {
        long result = 0;

        for (char c : code.toCharArray()) {
            result = result * BASE + CHARS.indexOf(c);
        }

        return result;
    }
}