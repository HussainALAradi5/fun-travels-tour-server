package com.server.server.dto.agency;

import com.server.server.dto.user.UserResponse.AgencyBranchSummary;
import com.server.server.dto.user.UserResponse.AgencySummary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyResponse {
    private Integer id;
    private String agencyName;
    private String address;
    private String contactNumber;
    private String ownerMobileNumber;
    private CountrySummary country;
    private CitySummary city;
    private UserSummary agencyOwner;
    private boolean active;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CountrySummary {
        private Integer id;
        private String famousName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CitySummary {
        private Integer id;
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String name;
        private String email;
    }
}
