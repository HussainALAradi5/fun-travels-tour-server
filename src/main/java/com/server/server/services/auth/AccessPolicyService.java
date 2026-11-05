package com.server.server.services.auth;

import java.util.Objects;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.constants.RoleGroups;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.exceptions.AccessDeniedException;
import com.server.server.models.agency.Agency;
import com.server.server.models.user.User;
import com.server.server.repositories.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central ownership and tenancy rules. Controllers check roles; this service checks
 * whose data a signed-in user may read or change.
 */
@Service
@RequiredArgsConstructor
public class AccessPolicyService {

    private final UserRepository userRepository;

    /** The signed-in, active user, or {@code null} for anonymous requests. */
    @Transactional(readOnly = true)
    public User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Your account is inactive or no longer exists."));
    }

    public User requireCurrentUser() {
        User user = currentUserOrNull();
        if (user == null) {
            throw new AccessDeniedException("Sign in to continue.");
        }
        return user;
    }

    public static boolean isAdmin(User user) {
        return user != null && user.getUserType() == UserTypeEnum.ADMIN;
    }

    public static boolean isCustomer(User user) {
        return user != null && (user.getUserType() == null || user.getUserType() == UserTypeEnum.CUSTOMER);
    }

    public static boolean isAgencyStaff(User user) {
        return user != null && RoleGroups.AGENCY_STAFF.contains(user.getUserType());
    }

    /** Managers and employees, whose tour lists are narrowed to their branch. */
    public static boolean isBranchStaff(User user) {
        return user != null && RoleGroups.BRANCH_STAFF.contains(user.getUserType());
    }

    public static boolean isSupportStaff(User user) {
        return user != null && RoleGroups.SUPPORT_STAFF.contains(user.getUserType());
    }

    /** Agency id that limits what this user may see; {@code null} means no agency limit. */
    public static Integer agencyScope(User user) {
        if (!isAgencyStaff(user)) {
            return null;
        }
        if (user.getAgency() == null) {
            throw new AccessDeniedException("Your staff account is not linked to an agency.");
        }
        return user.getAgency().getId();
    }

    /** Allows the account owner and administrators. */
    public void requireSelfOrAdmin(Integer userId) {
        User current = requireCurrentUser();
        if (!isAdmin(current) && !Objects.equals(current.getId(), userId)) {
            throw new AccessDeniedException("You can only access your own account.");
        }
    }

    /** Customers may only touch their own records; agency staff only their agency's records. */
    public void requireRecordAccess(User current, Integer ownerId, Agency agency) {
        if (isCustomer(current)) {
            if (!Objects.equals(current.getId(), ownerId)) {
                throw new AccessDeniedException("You are not allowed to access this record.");
            }
            return;
        }
        requireAgencyAccess(current, agency);
    }

    /**
     * Stricter than {@link #requireAgencyAccess}: only administrators and staff of this agency pass. Used for
     * agency internals (branches, staff lists, owner contacts) that customers and other roles must not read.
     */
    public void requireAgencyMember(User current, Agency agency) {
        if (isAdmin(current)) {
            return;
        }
        if (!isAgencyStaff(current) || agency == null || current.getAgency() == null
                || !Objects.equals(current.getAgency().getId(), agency.getId())) {
            throw new AccessDeniedException("Only this agency's staff can see its details.");
        }
    }

    /** Agency staff may only act inside their own agency; other staff roles are not agency-bound. */
    public void requireAgencyAccess(User current, Agency agency) {
        Integer scope = agencyScope(current);
        if (scope != null && (agency == null || !Objects.equals(scope, agency.getId()))) {
            throw new AccessDeniedException("This record belongs to another agency.");
        }
    }
}
