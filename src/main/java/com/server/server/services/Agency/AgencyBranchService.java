package com.server.server.services.agency;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.server.enums.UserTypeEnum;
import com.server.server.models.User;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.repositories.agency.AgencyRepository;

@Service
public class AgencyBranchService {

    @Autowired
    private AgencyBranchRepository branchRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AgencyBranch> getAllBranches() {
        return branchRepository.findAll();
    }

    public List<AgencyBranch> getBranchesByAgency(Integer agencyId) {
        return branchRepository.findByAgencyIdAndIsActiveTrue(agencyId);
    }

    public AgencyBranch addBranch(Integer agencyId, AgencyBranch branch) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found with id: " + agencyId));

        boolean exists = branchRepository.existsByBranchNameIgnoreCaseAndAgencyId(
                branch.getBranchName().trim(),
                agencyId);

        if (exists) {
            throw new RuntimeException("A branch named '" + branch.getBranchName() +
                    "' already exists for " + agency.getAgencyName());
        }

        branch.setAgency(agency);
        AgencyBranch savedBranch = branchRepository.save(branch);
        if (savedBranch.getBranchManager() != null && savedBranch.getBranchManager().getId() != null) {
            User manager = userRepository.findById(savedBranch.getBranchManager().getId())
                    .orElseThrow(() -> new RuntimeException("User assigned as Manager not found."));

            manager.setAgency(agency);
            manager.setAgencyBranch(savedBranch);
            userRepository.save(manager);
        }

        return savedBranch;
    }

    public List<User> getEmployeesByBranch(Integer agencyId, Integer branchId) {
        AgencyBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        if (!branch.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Security Alert: This branch does not belong to the specified agency.");
        }

        return userRepository.findByAgencyBranchIdAndUserTypeAndIsActiveTrue(branchId, UserTypeEnum.EMPLOYEE);
    }
}