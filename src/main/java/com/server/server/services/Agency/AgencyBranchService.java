package com.server.server.services.agency;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.UserTypeEnum;
import com.server.server.exceptions.DuplicateResourceException;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.User;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.repositories.agency.AgencyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgencyBranchService {

    private final AgencyBranchRepository branchRepository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AgencyBranch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AgencyBranch> getBranchesByAgency(@NonNull Integer agencyId) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        return branchRepository.findByAgencyIdAndIsActiveTrue(agencyId);
    }

    @Transactional
    public AgencyBranch addBranch(@NonNull Integer agencyId, AgencyBranch branch) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));

        boolean exists = branchRepository.existsByBranchNameIgnoreCaseAndAgencyId(
                branch.getBranchName().trim(), agencyId);

        if (exists) {
            throw new DuplicateResourceException("A branch named '" + branch.getBranchName() +
                    "' already exists for " + agency.getAgencyName());
        }

        branch.setAgency(agency);
        AgencyBranch savedBranch = branchRepository.save(branch);

        if (savedBranch.getBranchManager() != null && savedBranch.getBranchManager().getId() != null) {
            User manager = userRepository.findById(savedBranch.getBranchManager().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", savedBranch.getBranchManager().getId()));

            manager.setAgency(agency);
            manager.setAgencyBranch(savedBranch);
            userRepository.save(manager);
        }

        return savedBranch;
    }

    @Transactional(readOnly = true)
    public List<User> getEmployeesByBranch(@NonNull Integer agencyId, @NonNull Integer branchId) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        Objects.requireNonNull(branchId, "branchId must not be null");
        AgencyBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        if (!branch.getAgency().getId().equals(agencyId)) {
            throw new SecurityException("This branch does not belong to the specified agency.");
        }

        return userRepository.findByAgencyBranchIdAndUserTypeAndIsActiveTrue(branchId, UserTypeEnum.EMPLOYEE);
    }
}
