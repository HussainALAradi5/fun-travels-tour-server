package com.server.server.constants.user;

import java.util.EnumSet;
import java.util.Set;

import com.server.server.enums.user.UserTypeEnum;

/** Role sets used by service-level authorization rules. */
public final class RoleGroups {

    /** Staff who belong to one agency and only see that agency's data. */
    public static final Set<UserTypeEnum> AGENCY_STAFF = EnumSet.of(
            UserTypeEnum.OWNER, UserTypeEnum.MANAGER, UserTypeEnum.EMPLOYEE);

    /** Agency staff tied to one branch; their lists are narrowed to it. */
    public static final Set<UserTypeEnum> BRANCH_STAFF = EnumSet.of(UserTypeEnum.MANAGER, UserTypeEnum.EMPLOYEE);
    /** Staff who handle support requests across the platform. */
    public static final Set<UserTypeEnum> SUPPORT_STAFF = EnumSet.of(
            UserTypeEnum.ADMIN, UserTypeEnum.SUPPORT_AGENT);

    /** Roles an owner may create inside their own agency. */
    public static final Set<UserTypeEnum> OWNER_CREATABLE = EnumSet.of(
            UserTypeEnum.MANAGER, UserTypeEnum.EMPLOYEE);

    /** Roles a manager may create inside their own agency. */
    public static final Set<UserTypeEnum> MANAGER_CREATABLE = EnumSet.of(UserTypeEnum.EMPLOYEE);

    /** Staff who may export agency and fleet reports (agency staff only see their own agency). */
    public static final Set<UserTypeEnum> REPORT_STAFF = EnumSet.of(
            UserTypeEnum.ADMIN, UserTypeEnum.OWNER, UserTypeEnum.MANAGER, UserTypeEnum.EMPLOYEE);
    /** Roles that may export the user directory, matching {@code GET /api/users}. */
    public static final Set<UserTypeEnum> USER_REPORT = EnumSet.of(UserTypeEnum.ADMIN, UserTypeEnum.MANAGER);
    /** Roles accepted from the employee bulk import file. */
    public static final Set<UserTypeEnum> IMPORTABLE = EnumSet.of(
            UserTypeEnum.MANAGER, UserTypeEnum.EMPLOYEE);

    private RoleGroups() {
    }
}
