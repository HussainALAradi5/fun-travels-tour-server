package com.server.server.dto.user;

import com.server.server.enums.UserTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Integer id;
    private String userName;
    private String name;
    private String email;
    private String mobileNumber;
    private Integer age;
    private UserTypeEnum userType;
    private String profileImageUrl;
    private boolean active;
    private AgencySummary agency;
    private AgencyBranchSummary agencyBranch;

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
    public static class AgencyBranchSummary {
        private Integer id;
        private String branchName;
    }
}
