package com.server.server.constants.geography;

import java.util.Map;

/**
 * Digits in a mobile number after the country code, for countries the app's users commonly come from. The country
 * directory has no number lengths; countries missing here leave the field empty and accept the general limits.
 */
public final class MobileNumberLengths {

    public static final Map<String, Integer> BY_COUNTRY_CODE = Map.ofEntries(
            Map.entry("BH", 8), Map.entry("SA", 9), Map.entry("AE", 9), Map.entry("KW", 8), Map.entry("QA", 8),
            Map.entry("OM", 8), Map.entry("JO", 9), Map.entry("EG", 10), Map.entry("LB", 8), Map.entry("IQ", 10),
            Map.entry("TR", 10), Map.entry("IN", 10), Map.entry("PK", 10), Map.entry("PH", 10), Map.entry("GB", 10),
            Map.entry("US", 10));

    private MobileNumberLengths() {
    }
}
