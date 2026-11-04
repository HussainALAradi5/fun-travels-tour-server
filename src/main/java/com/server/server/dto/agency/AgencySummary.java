package com.server.server.dto.agency;

import com.server.server.models.agency.Agency;
/** An agency by id and name. */
public record AgencySummary(Integer id, String agencyName) {

    public static AgencySummary from(Agency source) {
        return source == null ? null : new AgencySummary(source.getId(), source.getAgencyName());
    }
}
