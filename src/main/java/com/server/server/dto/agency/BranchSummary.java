package com.server.server.dto.agency;

import com.server.server.models.agency.AgencyBranch;
/** An agency branch by id and name. */
public record BranchSummary(Integer id, String branchName) {

    public static BranchSummary from(AgencyBranch source) {
        return source == null ? null : new BranchSummary(source.getId(), source.getBranchName());
    }
}
