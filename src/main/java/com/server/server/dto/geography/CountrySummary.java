package com.server.server.dto.geography;

import com.server.server.models.geography.Country;
import com.server.server.utilities.geography.FlagUrls;

/** A country with the names and flag that lists and cards display. */
public record CountrySummary(Integer id, String famousName, String officialName, String flagPngUrl) {

    public static CountrySummary from(Country source) {
        return source == null ? null : new CountrySummary(source.getId(), source.getFamousName(), source.getOfficialName(),
                FlagUrls.orDefault(source.getFlagPngUrl(), FlagUrls.png(source.getCountryCode())));
    }
}
