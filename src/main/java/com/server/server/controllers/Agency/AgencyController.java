package com.server.server.controllers.agency;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.server.server.models.agency.Agency;
import com.server.server.models.User;
import com.server.server.services.agency.AgencyService;
import com.server.server.utilities.ApiResponse;

@RestController
@RequestMapping("/api/agencies")
@CrossOrigin(origins = "*")
public class AgencyController {

    @Autowired
    private AgencyService agencyService;

    @PostMapping
    public ResponseEntity<ApiResponse<Agency>> create(@RequestBody Map<String, Object> payload) {
        // Pass the raw map to the service.
        // Logic for extraction and object building is now in the Service.
        Agency created = agencyService.createAgencyFromMap(payload);
        return ResponseEntity.ok(new ApiResponse<>(true, "Agency created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Agency>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched agency", agencyService.getAgencyById(id)));
    }

@GetMapping("/{id}/employees")
public ResponseEntity<ApiResponse<List<User>>> getEmployeesByAgency(@PathVariable Integer id) {
    // Assuming you have this method in your service
    List<User> employees = agencyService.getEmployeesByAgencyId(id); 
    return ResponseEntity.ok(new ApiResponse<>(true, "Fetched agency employees", employees));
}

    @GetMapping
    public ResponseEntity<ApiResponse<List<Agency>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all agencies", agencyService.getAllAgencies()));
    }
}