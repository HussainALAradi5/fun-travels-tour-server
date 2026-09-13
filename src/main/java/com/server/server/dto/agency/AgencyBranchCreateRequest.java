package com.server.server.dto.agency;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyBranchCreateRequest {
    @NotBlank(message = "Branch name is required")
    private String branchName;
    private String branchAddress;
    private String contactNumber;
    private Integer branchManagerId;
}
