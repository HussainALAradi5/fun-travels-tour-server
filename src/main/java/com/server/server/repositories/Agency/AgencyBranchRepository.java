package com.server.server.repositories.agency;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.models.agency.AgencyBranch;

public interface AgencyBranchRepository extends JpaRepository<AgencyBranch, Integer> {
    List<AgencyBranch> findByAgencyIdAndIsActiveTrue(Integer agencyId);

    List<AgencyBranch> findByIsActiveTrue();

    Optional<AgencyBranch> findByBranchNameIgnoreCaseAndAgencyId(String branchName, Integer agencyId);

    boolean existsByBranchNameIgnoreCaseAndAgencyId(String branchName, Integer agencyId);
}
