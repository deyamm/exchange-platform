package com.exchange.platform.common.core.util;

import java.security.SecureRandom;

public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private IdGenerator() {}

    public static String generate(String prefix) {
        String normalized = normalizePrefix(prefix);
        return normalized + "-" + System.currentTimeMillis() + "-" + randomString(10);
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return randomLetters(4);
        }
        String trimmed = prefix.trim().toUpperCase();
        if (trimmed.length() >= 3 && trimmed.length() <= 5 && trimmed.matches("[A-Z]+")) {
            return trimmed;
        }
        return randomLetters(4);
    }

    private static String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return builder.toString();
    }

    private static String randomLetters(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append((char) ('A' + RANDOM.nextInt(26)));
        }
        return builder.toString();
    }
}
