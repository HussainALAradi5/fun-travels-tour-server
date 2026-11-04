package com.server.server.utilities.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.server.server.enums.report.ReportType;
import com.server.server.enums.user.UserTypeEnum;

class ReportTypeTest {

    @Test
    void customersMayExportOnlyTheirOwnLists() {
        assertThat(ReportType.TICKETS.allows(UserTypeEnum.CUSTOMER)).isTrue();
        assertThat(ReportType.TRANSACTIONS.allows(UserTypeEnum.CUSTOMER)).isTrue();
        assertThat(ReportType.TOURS.allows(UserTypeEnum.CUSTOMER)).isFalse();
        assertThat(ReportType.USERS.allows(UserTypeEnum.CUSTOMER)).isFalse();
        assertThat(ReportType.PASSENGER_MANIFEST.allows(UserTypeEnum.CUSTOMER)).isFalse();
    }

    @Test
    void userDirectoryFollowsTheUsersEndpointRoles() {
        assertThat(ReportType.USERS.allows(UserTypeEnum.ADMIN)).isTrue();
        assertThat(ReportType.USERS.allows(UserTypeEnum.MANAGER)).isTrue();
        assertThat(ReportType.USERS.allows(UserTypeEnum.EMPLOYEE)).isFalse();
    }
}
