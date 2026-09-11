package com.server.server.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.server.server.enums.UserRequest.UserRequestStatus;
import com.server.server.enums.UserRequest.UserRequestType;
import com.server.server.dto.filter.UserRequestFilterRequest;
import com.server.server.models.UserRequest;
import com.server.server.services.UserRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserRequestController {

    private final UserRequestService service;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody UserRequest request) {
        return execute(() -> service.create(request), "Request submitted successfully!", HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserRequest>> getRequests(@ModelAttribute UserRequestFilterRequest filter) {
        // GET lists usually return the array directly for performance
        return ResponseEntity.ok(service.getFilteredRequests(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return execute(() -> service.getById(id), null, HttpStatus.OK);
    }

    @PatchMapping("/{id}/assign/{agentId}")
    public ResponseEntity<?> assign(@PathVariable Integer id, @PathVariable Integer agentId) {
        return execute(() -> service.assignRequest(id, agentId), "Agent assigned to request.", HttpStatus.OK);
    }

    @PatchMapping("/{id}/solve/{solverId}")
    public ResponseEntity<?> solve(@PathVariable Integer id, @PathVariable Integer solverId) {
        return execute(() -> service.solveRequest(id, solverId), "Request marked as solved!", HttpStatus.OK);
    }

    /**
     * REJECT ACTION
     */
    @PatchMapping("/{id}/reject/{rejectedById}")
    public ResponseEntity<?> reject(@PathVariable Integer id, @PathVariable Integer rejectedById) {
        return execute(() -> service.rejectRequest(id, rejectedById), "Request has been rejected.", HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return execute(() -> {
            service.delete(id);
            return null;
        }, "Request deleted successfully", HttpStatus.OK);
    }

    // Unified Response Wrapper
    private ResponseEntity<Map<String, Object>> execute(ServiceAction action, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        try {
            Object data = action.run();
            if (message != null)
                response.put("message", message);
            if (data != null)
                response.put("data", data);
            response.put("success", true);
            return new ResponseEntity<>(response, status);
        } catch (Exception e) {
            response.put("message", e.getMessage());
            response.put("success", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @FunctionalInterface
    interface ServiceAction {
        Object run() throws Exception;
    }
}
