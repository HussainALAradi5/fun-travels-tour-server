package com.server.server.dto.agency;

import com.server.server.dto.user.UserSummary;
import com.server.server.models.agency.AgencyBranch;
public record AgencyBranchResponse(
        Integer id,
        String branchName,
        String branchAddress,
        String contactNumber,
        AgencySummary agency,
        UserSummary branchManager,
        boolean active) {

    public static AgencyBranchResponse from(AgencyBranch branch) {
        if (branch == null) {
            return null;
        }
        return new AgencyBranchResponse(branch.getId(), branch.getBranchName(), branch.getBranchAddress(),
                branch.getContactNumber(), AgencySummary.from(branch.getAgency()),
                UserSummary.from(branch.getBranchManager()), branch.isActive());
    }
}
