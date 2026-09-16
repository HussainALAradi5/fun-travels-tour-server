package com.server.server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.server.server.dto.PageResponse;
import com.server.server.dto.filter.UserRequestFilterRequest;
import com.server.server.dto.support.UserRequestCreateRequest;
import com.server.server.dto.support.UserRequestResponse;
import com.server.server.services.UserRequestService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserRequestController {

    private final UserRequestService service;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<UserRequestResponse>> create(
            @Valid @RequestBody UserRequestCreateRequest request) {
        UserRequestResponse response = modelMapper.toUserRequestResponse(service.create(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Request submitted successfully!", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserRequestResponse>>> getRequests(
            @ModelAttribute UserRequestFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getFilteredRequests(filter).map(modelMapper::toUserRequestResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRequestResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toUserRequestResponse(service.getById(id))));
    }

    @PatchMapping("/{id}/assign/{agentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<ApiResponse<UserRequestResponse>> assign(@PathVariable Integer id, @PathVariable Integer agentId) {
        return ResponseEntity.ok(ApiResponse.ok("Agent assigned to request.",
                modelMapper.toUserRequestResponse(service.assignRequest(id, agentId))));
    }

    @PatchMapping("/{id}/solve")
    public ResponseEntity<ApiResponse<UserRequestResponse>> solve(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Request marked as solved!",
                modelMapper.toUserRequestResponse(service.solveRequest(id))));
    }

    /**
     * REJECT ACTION
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<ApiResponse<UserRequestResponse>> reject(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Request has been rejected.",
                modelMapper.toUserRequestResponse(service.rejectRequest(id))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Request deleted successfully"));
    }
}
