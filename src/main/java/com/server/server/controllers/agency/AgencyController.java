package com.server.server.controllers.agency;

import com.server.server.dto.agency.AgencyCreateRequest;
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

import com.server.server.dto.agency.AgencyResponse;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.user.UserResponse;
import com.server.server.models.user.User;
import com.server.server.models.agency.Agency;
import com.server.server.services.agency.AgencyService;
import com.server.server.utilities.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/agencies")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<AgencyResponse>> create(@Valid @RequestBody AgencyCreateRequest request) {
        Agency created = agencyService.createAgency(request);
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Agency created successfully", AgencyResponse.from(created)));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<AgencyResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched agency",
                AgencyResponse.from(agencyService.getVisibleAgency(id))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<AgencyResponse>>> search(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched matching agencies",
                agencyService.searchAgencies(query, page, size).map(AgencyResponse::from)));
    }

    @GetMapping("/{id:\\d+}/employees")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getEmployeesByAgency(@NonNull @PathVariable Integer id) {
        List<User> employees = agencyService.getEmployeesByAgencyId(id);
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Fetched agency employees", employees.stream().map(UserResponse::from).toList()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AgencyResponse>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all agencies",
                agencyService.getAllAgencies().stream().map(AgencyResponse::from).toList()));
    }
}
