package com.server.server.controllers.agency;

import com.server.server.dto.agency.AgencyBranchCreateRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.agency.AgencyBranchResponse;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.user.UserResponse;
import com.server.server.models.user.User;
import com.server.server.services.agency.AgencyBranchService;
import com.server.server.utilities.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class AgencyBranchController {

    private final AgencyBranchService branchService;

    @PostMapping("/agency/{agencyId:\\d+}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<AgencyBranchResponse>> addBranch(
            @NonNull @PathVariable Integer agencyId,
            @Valid @RequestBody AgencyBranchCreateRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Branch added",
                AgencyBranchResponse.from(branchService.addBranch(agencyId, request))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<AgencyBranchResponse>>> getAllBranches() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all branches",
                branchService.getAllBranches().stream().map(AgencyBranchResponse::from).toList()));
    }

    @GetMapping("/agency/{agencyId:\\d+}")
    public ResponseEntity<ApiResponse<List<AgencyBranchResponse>>> getByAgency(
            @NonNull @PathVariable Integer agencyId) {
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Fetched agency branches",
                        branchService.getBranchesByAgency(agencyId).stream().map(AgencyBranchResponse::from).toList()));
    }

    @GetMapping("/agency/{agencyId:\\d+}/search")
    public ResponseEntity<ApiResponse<PageResponse<AgencyBranchResponse>>> searchByAgency(
            @NonNull @PathVariable Integer agencyId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched matching agency branches",
                branchService.searchBranches(agencyId, query, page, size)
                        .map(AgencyBranchResponse::from)));
    }

    @GetMapping("/agency/{agencyId:\\d+}/branch/{branchId:\\d+}/employees")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getEmployeesByAgencyAndBranch(
            @NonNull @PathVariable Integer agencyId,
            @NonNull @PathVariable Integer branchId) {

        List<User> employees = branchService.getEmployeesByBranch(agencyId, branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched employees for specific agency branch",
                employees.stream().map(UserResponse::from).toList()));
    }
}
