package com.server.server.dto.agency;

import jakarta.validation.constraints.NotBlank;
public record AgencyBranchCreateRequest(
        @NotBlank(message = "Branch name is required") String branchName,
        String branchAddress,
        String contactNumber,
        Integer branchManagerId) {
}
