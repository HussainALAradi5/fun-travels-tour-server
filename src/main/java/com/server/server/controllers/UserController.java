package com.server.server.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.server.server.dto.user.UserResponse;
import com.server.server.enums.UserTypeEnum;
import com.server.server.models.User;
import com.server.server.services.UserService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toUserResponseList(userService.getUsersByType(null))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toUserResponse(userService.getUserById(id))));
    }

    @GetMapping("/agency/{agencyId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getEmployeesByAgency(
            @NonNull @PathVariable Integer agencyId,
            @RequestParam(required = false) UserTypeEnum role) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toUserResponseList(userService.getAgencyUsers(agencyId, role))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@NonNull @PathVariable Integer id, @Valid @RequestBody User user) {
        return ResponseEntity.ok(ApiResponse.ok("User updated!", modelMapper.toUserResponse(userService.updateUser(id, user))));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    @PostMapping("/bulk-import")
    public ResponseEntity<ApiResponse<List<UserResponse>>> bulkImport(
            @RequestParam("file") MultipartFile file,
            @NonNull @RequestParam Integer agencyId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bulk import successful!", modelMapper.toUserResponseList(userService.bulkImportEmployees(file, agencyId))));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/permissions/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updatePermissions(
            @NonNull @PathVariable Integer id,
            @RequestParam UserTypeEnum type,
            @RequestParam(required = false) Integer branchId) {
        return ResponseEntity.ok(ApiResponse.ok("Permissions updated!", modelMapper.toUserResponse(userService.updatePermissions(id, type, branchId))));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER', 'MANAGER')")
    @PostMapping("/add-employee")
    public ResponseEntity<ApiResponse<UserResponse>> addEmployee(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee added!", modelMapper.toUserResponse(userService.createUser(user))));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@NonNull @PathVariable Integer id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated successfully"));
    }

    @GetMapping("/role/{type}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable UserTypeEnum type) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toUserResponseList(userService.getUsersByType(type))));
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(
            @RequestParam String identifier,
            @RequestParam(required = false) String baseNumber) {
        return ResponseEntity.ok(ApiResponse.ok(userService.requestPasswordReset(identifier, baseNumber)));
    }

    @PostMapping("/confirm-password-reset")
    public ResponseEntity<ApiResponse<UserResponse>> confirmPasswordReset(
            @RequestParam String identifier,
            @RequestParam(required = false) String baseNumber,
            @RequestParam String token,
            @RequestParam String newPassword) {
        return ResponseEntity.ok(ApiResponse.ok("Password reset!", modelMapper.toUserResponse(userService.confirmPasswordReset(identifier, baseNumber, token, newPassword))));
    }
}
