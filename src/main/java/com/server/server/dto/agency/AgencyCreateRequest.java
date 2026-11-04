package com.server.server.dto.agency;

import jakarta.validation.constraints.NotBlank;
public record AgencyCreateRequest(
        @NotBlank(message = "Agency name is required") String agencyName,
        String address,
        String contactNumber,
        String ownerMobileNumber,
        Integer countryId,
        Integer cityId,
        Integer agencyOwnerId) {
}
