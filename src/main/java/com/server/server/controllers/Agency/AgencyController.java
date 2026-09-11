package com.server.server.controllers.agency;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import com.server.server.dto.agency.AgencyResponse;
import com.server.server.dto.user.UserResponse;
import com.server.server.models.Agency.Agency;
import com.server.server.models.User;
import com.server.server.services.agency.AgencyService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgencyController {

    private final AgencyService agencyService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<AgencyResponse>> create(@RequestBody Map<String, Object> payload) {
        Agency created = agencyService.createAgencyFromMap(payload);
        return ResponseEntity.ok(new ApiResponse<>(true, "Agency created successfully", modelMapper.toAgencyResponse(created)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgencyResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched agency", modelMapper.toAgencyResponse(agencyService.getAgencyById(id))));
    }

    @GetMapping("/{id}/employees")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getEmployeesByAgency(@NonNull @PathVariable Integer id) {
        List<User> employees = agencyService.getEmployeesByAgencyId(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched agency employees", modelMapper.toUserResponseList(employees)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgencyResponse>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all agencies", modelMapper.toAgencyResponseList(agencyService.getAllAgencies())));
    }
}

