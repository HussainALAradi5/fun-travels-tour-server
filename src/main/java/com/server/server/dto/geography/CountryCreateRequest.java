package com.server.server.dto.geography;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Manual country entry; {@code countryCode} is ISO 3166-1 alpha-2. */
@Data
public class CountryCreateRequest {
    @NotBlank(message = "Country name is required")
    private String famousName;
    private String officialName;
    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country code must be two letters (ISO 3166-1 alpha-2)")
    private String countryCode;
    @Pattern(regexp = "^\\+?[0-9]{1,4}$", message = "Dial code must look like +973")
    private String dialCode;
    @Positive(message = "Mobile number length must be positive")
    private Integer mobileNumberLength;
    private String flagPngUrl;
    private String flagSvgUrl;
}
