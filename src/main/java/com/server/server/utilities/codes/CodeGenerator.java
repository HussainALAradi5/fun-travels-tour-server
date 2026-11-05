package com.server.server.utilities.codes;

import java.security.SecureRandom;
import java.util.Locale;

/** Generates unique, human-readable codes such as {@code TKT-7KQ2M9XA4B}. */
public final class CodeGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int DEFAULT_TOKEN_LENGTH = 10;
    private static final int MIN_TOKEN_LENGTH = 4;
    private static final int PREFIX_LENGTH = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    public static String generate(String prefix) {
        return generate(prefix, DEFAULT_TOKEN_LENGTH);
    }

    public static String generate(String prefix, int tokenLength) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("A code prefix is required.");
        }
        if (tokenLength < MIN_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Code tokens must be at least " + MIN_TOKEN_LENGTH + " characters.");
        }
        StringBuilder code = new StringBuilder(prefix.trim().toUpperCase(Locale.ROOT)).append('-');
        for (int i = 0; i < tokenLength; i++) {
            code.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }

    /** Prefix derived from an entity class name, for example {@code Tour -> TOU}. */
    public static String prefixFor(Class<?> type) {
        String name = type.getSimpleName().toUpperCase(Locale.ROOT);
        return name.length() <= PREFIX_LENGTH ? name : name.substring(0, PREFIX_LENGTH);
    }
}
