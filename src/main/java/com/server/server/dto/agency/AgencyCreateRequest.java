package com.server.server.dto.agency;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyCreateRequest {
    @NotBlank(message = "Agency name is required")
    private String agencyName;
    private String address;
    private String contactNumber;
    private String ownerMobileNumber;
    private Integer countryId;
    private Integer cityId;
    private Integer agencyOwnerId;
}
