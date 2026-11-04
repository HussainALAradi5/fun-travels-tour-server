package com.server.server.dto.agency;

import com.server.server.dto.geography.CitySummary;
import com.server.server.dto.geography.CountrySummary;
import com.server.server.dto.user.UserContactSummary;
import com.server.server.models.agency.Agency;
public record AgencyResponse(
        Integer id,
        String agencyName,
        String address,
        String contactNumber,
        String ownerMobileNumber,
        CountrySummary country,
        CitySummary city,
        UserContactSummary agencyOwner,
        boolean active) {

    public static AgencyResponse from(Agency agency) {
        if (agency == null) {
            return null;
        }
        return new AgencyResponse(agency.getId(), agency.getAgencyName(), agency.getAddress(),
                agency.getContactNumber(), agency.getOwnerMobileNumber(), CountrySummary.from(agency.getCountry()),
                CitySummary.from(agency.getCity()), UserContactSummary.from(agency.getAgencyOwner()), agency.isActive());
    }
}
