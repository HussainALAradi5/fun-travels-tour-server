package com.server.server.utilities;

import java.util.List;
import java.util.regex.Pattern;

import com.server.server.constants.ValidationPatterns;

/** One canonical form for phone numbers ("+" country code, then digits), so storage, uniqueness, and login compare the same text. */
public final class MobileNumbers {

    private static final Pattern FORMATTING = Pattern.compile("[\s().-]");

    private MobileNumbers() {
    }

    /**
     * Removes spaces, dashes, dots, and brackets and turns a leading "00" into "+":
     * "+973 3999-0001" and "0097339990001" become "+97339990001". A number typed without its
     * country code stays without it, so validation rejects it instead of guessing a country.
     */
    public static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String compact = FORMATTING.matcher(raw.trim()).replaceAll("");
        return compact.startsWith("00") ? "+" + compact.substring(2) : compact;
    }

    /** Stored forms a sign-in identifier could match; empty when the text is not a phone number. */
    public static List<String> loginCandidates(String identifier) {
        String cleaned = clean(identifier);
        if (cleaned == null) {
            return List.of();
        }
        String withPlus = cleaned.startsWith("+") ? cleaned : "+" + cleaned;
        // Rows saved before the country code was required may lack the plus.
        return withPlus.matches(ValidationPatterns.MOBILE_NUMBER) ? List.of(withPlus, withPlus.substring(1)) : List.of();
    }
}
