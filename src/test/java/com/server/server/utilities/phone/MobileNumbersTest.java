package com.server.server.utilities.phone;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MobileNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = { "+97339990001", "+973 3999 0001", "+973-3999-0001", "+(973) 3999.0001", "0097339990001", " +97339990001 " })
    void formattingIsRemovedAndCountryCodeKept(String raw) {
        assertThat(MobileNumbers.clean(raw)).isEqualTo("+97339990001");
    }

    @Test
    void numberWithoutCountryCodeIsNotGivenOne() {
        assertThat(MobileNumbers.clean("39990001")).isEqualTo("39990001");
    }

    @ParameterizedTest
    @ValueSource(strings = { "+97339990001", "97339990001", "+973 3999 0001", "0097339990001" })
    void signInMatchesTheStoredNumberAndItsLegacyFormWithoutPlus(String typed) {
        assertThat(MobileNumbers.loginCandidates(typed)).containsExactly("+97339990001", "97339990001");
    }

    @ParameterizedTest
    @ValueSource(strings = { "customer", "customer@funtravel.com", "12345", "+0123456789", "" })
    void namesAndEmailsAreNotPhoneNumbers(String typed) {
        assertThat(MobileNumbers.loginCandidates(typed)).isEmpty();
    }

    @Test
    void nullIsHandled() {
        assertThat(MobileNumbers.clean(null)).isNull();
        assertThat(MobileNumbers.loginCandidates(null)).isEmpty();
    }
}
