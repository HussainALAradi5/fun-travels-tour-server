package com.server.server.dto.geography;

import com.server.server.models.geography.City;
public record CityResponse(Integer id, String name, CountrySummary country) {

    public static CityResponse from(City city) {
        return city == null ? null : new CityResponse(city.getId(), city.getName(), CountrySummary.from(city.getCountry()));
    }
}
