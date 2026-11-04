package com.server.server.dto.geography;

import com.server.server.models.geography.Country;
/** A country with the names and flag that lists and cards display. */
public record CountrySummary(Integer id, String famousName, String officialName, String flagPngUrl) {

    public static CountrySummary from(Country source) {
        return source == null ? null : new CountrySummary(source.getId(), source.getFamousName(), source.getOfficialName(), source.getFlagPngUrl());
    }
}
