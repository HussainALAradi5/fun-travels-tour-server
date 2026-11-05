package com.server.server.utilities.geography;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FlagUrlsTest {

    @Test
    void buildsHttpsFlagsFromTheCountryCode() {
        assertThat(FlagUrls.png("BH")).isEqualTo("https://flagcdn.com/w80/bh.png");
        assertThat(FlagUrls.svg("sa")).isEqualTo("https://flagcdn.com/sa.svg");
    }

    @Test
    void returnsNothingForInvalidCodes() {
        assertThat(FlagUrls.png(null)).isNull();
        assertThat(FlagUrls.png("BHR")).isNull();
        assertThat(FlagUrls.svg("1A")).isNull();
    }

    @Test
    void keepsAUsableAddressAndReplacesEmptyOnes() {
        assertThat(FlagUrls.orDefault("https://example.org/flag.png", "fallback")).isEqualTo("https://example.org/flag.png");
        assertThat(FlagUrls.orDefault(null, "fallback")).isEqualTo("fallback");
        assertThat(FlagUrls.orDefault(" ", "fallback")).isEqualTo("fallback");
        assertThat(FlagUrls.orDefault("null", "fallback")).isEqualTo("fallback");
    }
}
