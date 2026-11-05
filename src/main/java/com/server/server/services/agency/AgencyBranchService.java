package com.server.server.services.agency;

import com.server.server.services.auth.AccessPolicyService;

import com.server.server.dto.agency.AgencyBranchCreateRequest;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.user.UserTypeEnum;
import com.server.server.exceptions.common.DuplicateResourceException;
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.models.user.User;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.repositories.user.UserRepository;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.dto.common.PageResponse;
import com.server.server.utilities.pagination.PaginationUtils;
import java.util.Set;
import com.server.server.repositories.agency.AgencyRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgencyBranchService {

    private final AgencyBranchRepository branchRepository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final AccessPolicyService accessPolicy;

    @Transactional(readOnly = true)
    public List<AgencyBranch> getAllBranches() {
        return branchRepository.findAll();
    }

    /** Active branches, for administrators and the agency's own staff. */
    @Transactional(readOnly = true)
    public List<AgencyBranch> getBranchesByAgency(@NonNull Integer agencyId) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        requireMember(agencyId);
        return branchRepository.findByAgencyIdAndIsActiveTrue(agencyId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgencyBranch> searchBranches(
            @NonNull Integer agencyId, String query, Integer page, Integer size) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        requireMember(agencyId);
        String safeQuery = query == null ? "" : query.trim();
        var pageable = PaginationUtils.pageable(
                page, size, "branchName", "asc", "branchName", Set.of("branchName"));
        return PageResponse.from(safeQuery.isEmpty()
                ? branchRepository.findByAgencyId(agencyId, pageable)
                : branchRepository.findByAgencyIdAndBranchNameContainingIgnoreCase(
                        agencyId, safeQuery, pageable));
    }

    /** Admins add branches anywhere; owners only to their own agency. */
    @Transactional
    public AgencyBranch addBranch(@NonNull Integer agencyId, AgencyBranchCreateRequest request) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));
        accessPolicy.requireAgencyAccess(accessPolicy.requireCurrentUser(), agency);
        String name = request.branchName().trim();
        if (branchRepository.existsByBranchNameIgnoreCaseAndAgencyId(name, agencyId)) {
            throw new DuplicateResourceException("A branch named '" + name + "' already exists for " + agency.getAgencyName());
        }
        AgencyBranch branch = new AgencyBranch();
        branch.setBranchName(name);
        branch.setBranchAddress(request.branchAddress());
        branch.setContactNumber(request.contactNumber());
        branch.setAgency(agency);
        branch.setCountry(agency.getCountry());
        branch.setCity(agency.getCity());
        branch.setActive(true);
        AgencyBranch saved = branchRepository.save(branch);
        if (request.branchManagerId() != null) {
            User manager = userRepository.findById(request.branchManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.branchManagerId()));
            if (manager.getUserType() != UserTypeEnum.MANAGER) {
                throw new IllegalArgumentException("The branch manager must have the MANAGER role.");
            }
            // A manager of another agency is never moved over; only unassigned or own-agency managers qualify.
            if (manager.getAgency() != null && !Objects.equals(manager.getAgency().getId(), agencyId)) {
                throw new IllegalArgumentException("This manager belongs to another agency.");
            }
            manager.setAgency(agency);
            manager.setAgencyBranch(saved);
            userRepository.save(manager);
            saved.setBranchManager(manager);
        }
        return saved;
    }

    private void requireMember(Integer agencyId) {
        Agency agency = agencyRepository.findById(agencyId).orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));
        accessPolicy.requireAgencyMember(accessPolicy.requireCurrentUser(), agency);
    }

    @Transactional(readOnly = true)
    public List<User> getEmployeesByBranch(@NonNull Integer agencyId, @NonNull Integer branchId) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        Objects.requireNonNull(branchId, "branchId must not be null");
        requireMember(agencyId);
        AgencyBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        if (!branch.getAgency().getId().equals(agencyId)) {
            throw new ResourceNotFoundException("Branch", branchId);
        }

        return userRepository.findByAgencyBranchIdAndUserTypeAndIsActiveTrue(branchId, UserTypeEnum.EMPLOYEE);
    }
}
