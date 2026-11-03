package com.example.partnerquality.internal.text;

/**
 * Jaro-Winkler string similarity in [0, 1]. It suits short strings such as names: it tolerates typos and
 * transposed letters, and rewards a common prefix, because names rarely differ in their first letters.
 */
public final class JaroWinkler {

    private static final double PREFIX_SCALE = 0.1;
    private static final int MAX_PREFIX = 4;

    private JaroWinkler() {
    }

    public static double similarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        double jaro = jaro(s1, s2);
        int prefix = commonPrefixLength(s1, s2);
        return jaro + prefix * PREFIX_SCALE * (1 - jaro);
    }

    private static double jaro(String s1, String s2) {
        // Characters only count as matching when they are not further apart than this window.
        int window = Math.max(0, Math.max(s1.length(), s2.length()) / 2 - 1);
        boolean[] matched1 = new boolean[s1.length()];
        boolean[] matched2 = new boolean[s2.length()];

        int matches = 0;
        for (int i = 0; i < s1.length(); i++) {
            int from = Math.max(0, i - window);
            int to = Math.min(i + window + 1, s2.length());
            for (int j = from; j < to; j++) {
                if (!matched2[j] && s1.charAt(i) == s2.charAt(j)) {
                    matched1[i] = true;
                    matched2[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0.0;
        }

        // Matching characters that appear in a different order count as half a transposition each.
        int outOfOrder = 0;
        int j = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!matched1[i]) {
                continue;
            }
            while (!matched2[j]) {
                j++;
            }
            if (s1.charAt(i) != s2.charAt(j)) {
                outOfOrder++;
            }
            j++;
        }

        double m = matches;
        return (m / s1.length() + m / s2.length() + (m - outOfOrder / 2.0) / m) / 3.0;
    }

    private static int commonPrefixLength(String s1, String s2) {
        int limit = Math.min(MAX_PREFIX, Math.min(s1.length(), s2.length()));
        int prefix = 0;
        while (prefix < limit && s1.charAt(prefix) == s2.charAt(prefix)) {
            prefix++;
        }
        return prefix;
    }
}
