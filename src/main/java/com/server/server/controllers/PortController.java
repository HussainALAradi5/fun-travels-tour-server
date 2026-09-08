package com.server.server.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.geography.PortResponse;
import com.server.server.enums.GenericStatus;
import com.server.server.models.Port;
import com.server.server.services.PortService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ports")
@RequiredArgsConstructor
public class PortController {
    private final PortService portService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PortResponse>>> getAllActivePorts() {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toPortResponseList(portService.getAllActivePorts())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PortResponse>> createPort(@Valid @RequestBody Port port) {
        return ResponseEntity.ok(ApiResponse.ok("Port created!", modelMapper.toPortResponse(portService.createPort(port))));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PortResponse>> updateStatus(@PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated!", modelMapper.toPortResponse(portService.updateStatus(id, status))));
    }
}
