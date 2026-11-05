package com.server.server.enums.report;

import java.util.Set;

import com.server.server.constants.user.RoleGroups;
import com.server.server.enums.user.UserTypeEnum;

/**
 * Reports the server can generate, each with its title and the roles allowed to request it. Rows are
 * always loaded through the domain services, so customers and agency staff only see their own records.
 */
public enum ReportType {
    TOUR_CATALOG("Tour Catalog", null),
    TOURS("Tour Inventory", RoleGroups.REPORT_STAFF),
    PASSENGER_MANIFEST("Passenger Manifest", RoleGroups.REPORT_STAFF),
    TICKETS("Tickets", null),
    TRANSPORTATIONS("Fleet Inventory", RoleGroups.REPORT_STAFF),
    MEALS("Meal Plans", RoleGroups.REPORT_STAFF),
    TRANSACTIONS("Financial Ledger", null),
    USERS("System Users", RoleGroups.USER_REPORT),
    AGENCY_BRANCHES("Agency Branches", RoleGroups.REPORT_STAFF),
    AGENCY_EMPLOYEES("Agency Employees", RoleGroups.REPORT_STAFF);

    private final String title;
    /** Roles allowed to request the report; null means every signed-in user. */
    private final Set<UserTypeEnum> allowedRoles;

    ReportType(String title, Set<UserTypeEnum> allowedRoles) {
        this.title = title;
        this.allowedRoles = allowedRoles;
    }

    public String title() {
        return title;
    }

    public boolean allows(UserTypeEnum role) {
        return allowedRoles == null || allowedRoles.contains(role);
    }
}
