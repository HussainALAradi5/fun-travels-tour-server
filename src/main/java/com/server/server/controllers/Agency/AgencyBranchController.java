package com.server.server.controllers.agency;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.models.User;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.services.agency.AgencyBranchService;
import com.server.server.utilities.ApiResponse;

@RestController
@RequestMapping("/api/branches")
@CrossOrigin(origins = "*")
public class AgencyBranchController {

    @Autowired
    private AgencyBranchService branchService; // Use the new specific service

    @PostMapping("/agency/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyBranch>> addBranch(
            @PathVariable Integer agencyId,
            @RequestBody AgencyBranch branch) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Branch added", branchService.addBranch(agencyId, branch)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgencyBranch>>> getAllBranches() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all branches", branchService.getAllBranches()));
    }

    @GetMapping("/agency/{agencyId}")
    public ResponseEntity<ApiResponse<List<AgencyBranch>>> getByAgency(@PathVariable Integer agencyId) {
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Fetched agency branches", branchService.getBranchesByAgency(agencyId)));
    }

    @GetMapping("/agency/{agencyId}/branch/{branchId}/employees")
    public ResponseEntity<ApiResponse<List<User>>> getEmployeesByAgencyAndBranch(
            @PathVariable Integer agencyId,
            @PathVariable Integer branchId) {

        List<User> employees = branchService.getEmployeesByBranch(agencyId, branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched employees for specific agency branch", employees));
    }
}