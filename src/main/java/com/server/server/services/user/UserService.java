package com.server.server.services.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.server.server.utilities.MobileNumbers;
import com.server.server.constants.RoleGroups;
import com.server.server.dto.common.ImportResult;
import com.server.server.dto.user.EmployeeCreateRequest;
import com.server.server.dto.user.UserUpdateRequest;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.exceptions.AccessDeniedException;
import com.server.server.exceptions.DuplicateResourceException;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.models.user.User;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.repositories.agency.AgencyRepository;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.common.ExcelImportService;
import com.server.server.services.common.FileStorageService;
import com.server.server.services.payment.AccountService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int COLUMN_USERNAME = 0;
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_EMAIL = 2;
    private static final int COLUMN_PASSWORD = 3;
    private static final int COLUMN_AGE = 4;
    private static final int COLUMN_MOBILE = 5;
    private static final int COLUMN_ROLE = 6;
    private static final int COLUMN_BRANCH = 7;

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyBranchRepository branchRepository;
    private final ExcelImportService excelImportService;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final AccessPolicyService accessPolicy;

    /** Saves a new active user with a hashed password and opens their wallet. */
    @Transactional
    public User registerUser(User user, String rawPassword) {
        ensureUnique(user.getEmail(), user.getUserName(), user.getMobileNumber());
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getUserType() == null) {
            user.setUserType(UserTypeEnum.CUSTOMER);
        }
        user.setActive(true);
        User saved = userRepository.save(user);
        accountService.createAccountForUser(saved);
        return saved;
    }

    /**
     * Admins create any staff role. Owners create managers and employees in their agency;
     * managers create employees in their agency and branch.
     */
    @Transactional
    public User createEmployee(EmployeeCreateRequest request, Integer agencyId, Integer branchId) {
        User actor = accessPolicy.requireCurrentUser();
        UserTypeEnum role = request.userType();
        Agency agency;
        AgencyBranch branch = branchId == null ? null : branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        switch (actor.getUserType()) {
            case ADMIN -> agency = agencyId == null ? null : agencyRepository.findById(agencyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));
            case OWNER -> {
                requireRole(role, RoleGroups.OWNER_CREATABLE);
                agency = actor.getAgency();
            }
            case MANAGER -> {
                requireRole(role, RoleGroups.MANAGER_CREATABLE);
                agency = actor.getAgency();
                branch = actor.getAgencyBranch();
            }
            default -> throw new AccessDeniedException("You are not allowed to create staff accounts.");
        }
        if (RoleGroups.AGENCY_STAFF.contains(role) && agency == null) {
            throw new IllegalArgumentException("Agency staff must belong to an agency.");
        }
        if (branch != null && (agency == null || !Objects.equals(branch.getAgency().getId(), agency.getId()))) {
            throw new IllegalArgumentException("The selected branch does not belong to the agency.");
        }
        User user = new User();
        user.setUserName(request.userName().trim());
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setMobileNumber(request.mobileNumber().trim());
        user.setAge(Objects.requireNonNullElse(request.age(), 0));
        user.setUserType(role);
        user.setAgency(agency);
        user.setAgencyBranch(branch);
        return registerUser(user, request.password());
    }

    /** Users edit their own profile; admins edit anyone. Role and agency never change here. */
    @Transactional
    public User updateProfile(@NonNull Integer id, UserUpdateRequest request) {
        accessPolicy.requireSelfOrAdmin(id);
        User existing = getUserById(id);
        if (request.mobileNumber() != null && !request.mobileNumber().isBlank()) {
            String mobile = MobileNumbers.clean(request.mobileNumber());
            if (userRepository.existsByMobileNumberAndIdNot(mobile, id)) {
                throw new DuplicateResourceException("Mobile number already in use");
            }
            existing.setMobileNumber(mobile);
        }
        if (request.name() != null && !request.name().isBlank()) {
            existing.setName(request.name().trim());
        }
        if (request.age() != null) {
            existing.setAge(request.age());
        }
        if (request.removeImage()) {
            fileStorageService.deleteProfileImage(existing.getProfileImageUrl());
            existing.setProfileImageUrl(null);
        } else if (request.base64Image() != null && !request.base64Image().isBlank()) {
            existing.setProfileImageUrl(
                    fileStorageService.replaceProfileImage(request.base64Image(), id, existing.getProfileImageUrl()));
        }
        return userRepository.saveAndFlush(existing);
    }

    /**
     * Admin-only role and placement change. Agency staff must end up in an agency (taken from the
     * branch, or from {@code agencyId} for owners who have no branch); other roles leave any agency.
     */
    @Transactional
    public User updatePermissions(@NonNull Integer id, UserTypeEnum type, Integer agencyId, Integer branchId) {
        if (Objects.equals(accessPolicy.requireCurrentUser().getId(), id)) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }
        User user = getUserById(id);
        AgencyBranch branch = branchId == null ? null : branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        Agency agency = branch != null ? branch.getAgency() : agencyId == null ? null
                : agencyRepository.findById(agencyId).orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));
        if (branch != null && agencyId != null && !Objects.equals(branch.getAgency().getId(), agencyId)) {
            throw new IllegalArgumentException("The selected branch does not belong to the agency.");
        }
        boolean agencyStaff = RoleGroups.AGENCY_STAFF.contains(type);
        if (agencyStaff && agency == null) {
            throw new IllegalArgumentException("Agency staff must belong to an agency.");
        }
        user.setUserType(type);
        user.setAgency(agencyStaff ? agency : null);
        user.setAgencyBranch(agencyStaff ? branch : null);
        return userRepository.save(user);
    }

    @Transactional
    public void softDeleteUser(@NonNull Integer id) {
        if (Objects.equals(accessPolicy.requireCurrentUser().getId(), id)) {
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        }
        User user = getUserById(id);
        user.setActive(false);
        user.setAgencyBranch(null);
        userRepository.save(user);
    }

    /** Agency members are visible to admins and to staff of that agency. */
    @Transactional(readOnly = true)
    public List<User> getAgencyUsers(@NonNull Integer agencyId, UserTypeEnum type) {
        User actor = accessPolicy.requireCurrentUser();
        if (!AccessPolicyService.isAdmin(actor)) {
            Agency agency = agencyRepository.findById(agencyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));
            if (!AccessPolicyService.isAgencyStaff(actor)) {
                throw new AccessDeniedException("You are not allowed to view agency staff.");
            }
            accessPolicy.requireAgencyAccess(actor, agency);
        }
        return type == null
                ? userRepository.findByAgencyIdAndIsActiveTrue(agencyId)
                : userRepository.findByAgencyIdAndUserTypeAndIsActiveTrue(agencyId, type);
    }

    /**
     * Imports staff rows: username, name, email, password, age, mobile, role, branch name.
     * Managers import only into their own agency; only manager and employee roles are accepted.
     */
    @Transactional
    public ImportResult bulkImportEmployees(MultipartFile file, @NonNull Integer agencyId) {
        User actor = accessPolicy.requireCurrentUser();
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));
        accessPolicy.requireAgencyAccess(actor, agency);
        List<List<String>> rows = excelImportService.readRows(file);
        List<String> errors = new ArrayList<>();
        int imported = 0;
        for (int index = 0; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            try {
                registerUser(toImportedUser(row, agency, actor), required(row, COLUMN_PASSWORD, "password"));
                imported++;
            } catch (RuntimeException e) {
                errors.add("Row " + (index + 2) + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors.size(), errors);
    }

    /** Self, or any non-customer role (staff need to look up the customers they serve). */
    @Transactional(readOnly = true)
    public User getVisibleUser(@NonNull Integer id) {
        User actor = accessPolicy.requireCurrentUser();
        if (AccessPolicyService.isCustomer(actor) && !Objects.equals(actor.getId(), id)) {
            throw new AccessDeniedException("You can only view your own profile.");
        }
        return getUserById(id);
    }

    @Transactional(readOnly = true)
    public User getUserById(@NonNull Integer id) {
        return userRepository.findById(id).filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * Admins see every active user. Agency staff see their own agency's members, and customers when
     * they ask for that role, because staff sell walk-in tickets to customers.
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByType(UserTypeEnum type) {
        Integer scope = AccessPolicyService.agencyScope(accessPolicy.requireCurrentUser());
        if (scope == null || type == UserTypeEnum.CUSTOMER) {
            return type == null ? userRepository.findByIsActiveTrue() : userRepository.findByUserTypeAndIsActiveTrue(type);
        }
        return type == null ? userRepository.findByAgencyIdAndIsActiveTrue(scope)
                : userRepository.findByAgencyIdAndUserTypeAndIsActiveTrue(scope, type);
    }

    public User getCurrentUser() {
        return accessPolicy.currentUserOrNull();
    }

    private void ensureUnique(String email, String userName, String mobileNumber) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (userRepository.existsByUserNameIgnoreCase(userName)) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByMobileNumber(mobileNumber)) {
            throw new DuplicateResourceException("Mobile number already exists");
        }
    }

    private User toImportedUser(List<String> row, Agency agency, User actor) {
        String roleName = required(row, COLUMN_ROLE, "role").toUpperCase();
        UserTypeEnum role = Arrays.stream(UserTypeEnum.values())
                .filter(type -> type.name().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role " + roleName));
        boolean manager = actor.getUserType() == UserTypeEnum.MANAGER;
        requireRole(role, manager ? RoleGroups.MANAGER_CREATABLE : RoleGroups.IMPORTABLE);
        User user = new User();
        user.setUserName(required(row, COLUMN_USERNAME, "username"));
        user.setName(required(row, COLUMN_NAME, "name"));
        user.setEmail(required(row, COLUMN_EMAIL, "email"));
        user.setMobileNumber(required(row, COLUMN_MOBILE, "mobile number"));
        user.setAge(parseAge(cell(row, COLUMN_AGE)));
        user.setUserType(role);
        user.setAgency(agency);
        String branchName = cell(row, COLUMN_BRANCH);
        if (manager) {
            user.setAgencyBranch(actor.getAgencyBranch());
        } else if (!branchName.isEmpty()) {
            user.setAgencyBranch(branchRepository.findByBranchNameIgnoreCaseAndAgencyId(branchName, agency.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown branch " + branchName)));
        }
        return user;
    }

    private static void requireRole(UserTypeEnum role, java.util.Set<UserTypeEnum> allowed) {
        if (!allowed.contains(role)) {
            throw new AccessDeniedException("You are not allowed to assign the role " + role + ".");
        }
    }

    private static String cell(List<String> row, int index) {
        return row != null && index < row.size() && row.get(index) != null ? row.get(index).trim() : "";
    }

    private static String required(List<String> row, int index, String label) {
        String value = cell(row, index);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    private static int parseAge(String age) {
        try {
            return age.isEmpty() ? 0 : Integer.parseInt(age);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Age must be a whole number");
        }
    }
}
