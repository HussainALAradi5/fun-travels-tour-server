package com.server.server.dto.geography;

import com.server.server.models.geography.City;
/** A city by id and name. */
public record CitySummary(Integer id, String name) {

    public static CitySummary from(City source) {
        return source == null ? null : new CitySummary(source.getId(), source.getName());
    }
}
