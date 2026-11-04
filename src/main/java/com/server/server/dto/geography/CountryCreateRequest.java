package com.server.server.dto.geography;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Manual country entry; {@code countryCode} is ISO 3166-1 alpha-2. */
public record CountryCreateRequest(
        @NotBlank(message = "Country name is required") String famousName,
        String officialName,
        @NotBlank(message = "Country code is required") @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country code must be two letters (ISO 3166-1 alpha-2)") String countryCode,
        @Pattern(regexp = "^\\+?[0-9]{1,4}$", message = "Dial code must look like +973") String dialCode,
        @Positive(message = "Mobile number length must be positive") Integer mobileNumberLength,
        String flagPngUrl,
        String flagSvgUrl) {
}
