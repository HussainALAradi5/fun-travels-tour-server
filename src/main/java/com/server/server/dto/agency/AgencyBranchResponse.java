package com.server.server.dto.agency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyBranchResponse {
    private Integer id;
    private String branchName;
    private String branchAddress;
    private String contactNumber;
    private AgencySummary agency;
    private UserSummary branchManager;
    private boolean active;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgencySummary {
        private Integer id;
        private String agencyName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String name;
    }
}
