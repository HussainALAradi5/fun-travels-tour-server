package com.server.server.services.geography;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class CountryDirectoryServiceTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void joinsRootAndSingleSuffix() throws Exception {
        assertThat(CountryDirectoryService.dialCode(json.readTree("{\"root\":\"+9\",\"suffixes\":[\"73\"]}"))).isEqualTo("+973");
    }

    @Test
    void keepsSharedRootsWithManyAreaSuffixes() throws Exception {
        assertThat(CountryDirectoryService.dialCode(json.readTree("{\"root\":\"+1\",\"suffixes\":[\"201\",\"202\"]}"))).isEqualTo("+1");
        assertThat(CountryDirectoryService.dialCode(json.readTree("{\"root\":\"+7\",\"suffixes\":[\"6\",\"7\"]}"))).isEqualTo("+7");
    }

    @Test
    void usesTheShortestSuffixOtherwiseAndNothingWithoutARoot() throws Exception {
        assertThat(CountryDirectoryService.dialCode(json.readTree("{\"root\":\"+3\",\"suffixes\":[\"906698\",\"79\"]}"))).isEqualTo("+379");
        assertThat(CountryDirectoryService.dialCode(json.readTree("{}"))).isNull();
    }
}
