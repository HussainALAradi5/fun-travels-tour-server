package com.server.server.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.server.server.enums.UserTypeEnum;
import com.server.server.models.User;
import com.server.server.services.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getUsersByType(null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return execute(() -> userService.getUserById(id), null, HttpStatus.OK);
    }

    @GetMapping("/agency/{agencyId}")
    public ResponseEntity<List<User>> getEmployeesByAgency(
            @PathVariable Integer agencyId,
            @RequestParam(required = false) UserTypeEnum role) {
        return ResponseEntity.ok(userService.getAgencyUsers(agencyId, role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User user) {
        return execute(() -> userService.updateUser(id, user), "User updated successfully!", HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @PostMapping("/bulk-import")
    public ResponseEntity<?> bulkImport(@RequestParam("file") MultipartFile file, @RequestParam Integer agencyId) {
        return execute(() -> userService.bulkImportEmployees(file, agencyId), "Bulk import successful!",
                HttpStatus.CREATED);
    }

    @PutMapping("/permissions/{id}")
    public ResponseEntity<?> updatePermissions(@PathVariable Integer id, @RequestParam UserTypeEnum type,
            @RequestParam(required = false) Integer branchId) {
        return execute(() -> userService.updatePermissions(id, type, branchId), "Permissions updated!", HttpStatus.OK);
    }

    @PostMapping("/add-employee")
    public ResponseEntity<?> addEmployee(@RequestBody User user, @RequestParam(required = false) Integer agencyId,
            @RequestParam(required = false) Integer branchId, @RequestParam UserTypeEnum requesterType) {
        return execute(() -> userService.createUser(user), "Employee added!", HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        return execute(() -> {
            userService.softDeleteUser(id);
            return null;
        }, "User deactivated successfully", HttpStatus.OK);
    }

    @GetMapping("/role/{type}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable UserTypeEnum type) {
        return ResponseEntity.ok(userService.getUsersByType(type));
    }

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

@PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(
            @RequestParam String identifier, 
            @RequestParam(required = false) String baseNumber) {
        return execute(() -> userService.requestPasswordReset(identifier, baseNumber), 
            "Reset code sent successfully!", HttpStatus.OK);
    }

    @PostMapping("/confirm-password-reset")
    public ResponseEntity<?> confirmPasswordReset(
            @RequestParam String identifier, 
            @RequestParam(required = false) String baseNumber,
            @RequestParam String token, 
            @RequestParam String newPassword) {
        return execute(() -> userService.confirmPasswordReset(identifier, baseNumber, token, newPassword), 
            "Password reset successfully!", HttpStatus.OK);
    }
    @FunctionalInterface
    interface ServiceAction {
        Object run() throws Exception;
    }
}