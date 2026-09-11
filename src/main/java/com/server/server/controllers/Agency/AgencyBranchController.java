package com.server.server.controllers.agency;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.agency.AgencyBranchResponse;
import com.server.server.dto.user.UserResponse;
import com.server.server.models.User;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.services.agency.AgencyBranchService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgencyBranchController {

    private final AgencyBranchService branchService;
    private final ModelMapper modelMapper;

    @PostMapping("/agency/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyBranchResponse>> addBranch(
            @NonNull @PathVariable Integer agencyId,
            @RequestBody AgencyBranch branch) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Branch added",
                modelMapper.toBranchResponse(branchService.addBranch(agencyId, branch))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgencyBranchResponse>>> getAllBranches() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all branches",
                modelMapper.toBranchResponseList(branchService.getAllBranches())));
    }

    @GetMapping("/agency/{agencyId}")
    public ResponseEntity<ApiResponse<List<AgencyBranchResponse>>> getByAgency(
            @NonNull @PathVariable Integer agencyId) {
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Fetched agency branches",
                        modelMapper.toBranchResponseList(branchService.getBranchesByAgency(agencyId))));
    }

    @GetMapping("/agency/{agencyId}/branch/{branchId}/employees")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getEmployeesByAgencyAndBranch(
            @NonNull @PathVariable Integer agencyId,
            @NonNull @PathVariable Integer branchId) {

        List<User> employees = branchService.getEmployeesByBranch(agencyId, branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched employees for specific agency branch",
                modelMapper.toUserResponseList(employees)));
    }
}
