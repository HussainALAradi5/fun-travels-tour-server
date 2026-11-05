package com.server.server.utilities.geography;

import java.util.Locale;

/**
 * Flag images for an ISO 3166 alpha-2 country code, served over HTTPS by flagcdn.com. Used whenever a country has
 * no flag of its own, so clients never get an empty or broken image address.
 */
public final class FlagUrls {

    private static final String BASE = "https://flagcdn.com/";

    private FlagUrls() {
    }

    public static String png(String countryCode) {
        return isCode(countryCode) ? BASE + "w80/" + countryCode.toLowerCase(Locale.ROOT) + ".png" : null;
    }

    public static String svg(String countryCode) {
        return isCode(countryCode) ? BASE + countryCode.toLowerCase(Locale.ROOT) + ".svg" : null;
    }

    /** The given address when it is usable, otherwise the fallback. */
    public static String orDefault(String url, String fallback) {
        return url == null || url.isBlank() || "null".equals(url) ? fallback : url;
    }

    private static boolean isCode(String countryCode) {
        return countryCode != null && countryCode.matches("[A-Za-z]{2}");
    }
}
