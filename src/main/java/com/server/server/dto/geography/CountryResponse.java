package com.server.server.dto.geography;

import com.server.server.models.geography.Country;
import com.server.server.utilities.geography.FlagUrls;

public record CountryResponse(
        Integer id,
        String famousName,
        String officialName,
        String countryCode,
        String flagPngUrl,
        String flagSvgUrl,
        String dialCode,
        Integer mobileNumberLength) {

    public static CountryResponse from(Country country) {
        if (country == null) {
            return null;
        }
        return new CountryResponse(country.getId(), country.getFamousName(), country.getOfficialName(),
                country.getCountryCode(), FlagUrls.orDefault(country.getFlagPngUrl(), FlagUrls.png(country.getCountryCode())),
                FlagUrls.orDefault(country.getFlagSvgUrl(), FlagUrls.svg(country.getCountryCode())), country.getDialCode(),
                country.getMobileNumberLength());
    }
}
