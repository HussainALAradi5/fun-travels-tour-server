package com.server.server.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.server.server.enums.user.UserTypeEnum;
import com.server.server.exceptions.common.AccessDeniedException;
import com.server.server.models.agency.Agency;
import com.server.server.models.user.User;
import com.server.server.repositories.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccessPolicyServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AccessPolicyService accessPolicy;

    @Test
    void customerCanOnlyReachOwnRecords() {
        User customer = user(1, UserTypeEnum.CUSTOMER, null);
        assertThatCode(() -> accessPolicy.requireRecordAccess(customer, 1, agency(5))).doesNotThrowAnyException();
        assertThatThrownBy(() -> accessPolicy.requireRecordAccess(customer, 2, agency(5)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void agencyStaffAreLimitedToTheirAgency() {
        User manager = user(3, UserTypeEnum.MANAGER, agency(5));
        assertThatCode(() -> accessPolicy.requireRecordAccess(manager, 99, agency(5))).doesNotThrowAnyException();
        assertThatThrownBy(() -> accessPolicy.requireRecordAccess(manager, 99, agency(6)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(AccessPolicyService.agencyScope(manager)).isEqualTo(5);
    }

    @Test
    void adminHasNoAgencyLimit() {
        User admin = user(4, UserTypeEnum.ADMIN, null);
        assertThat(AccessPolicyService.agencyScope(admin)).isNull();
        assertThatCode(() -> accessPolicy.requireAgencyAccess(admin, agency(6))).doesNotThrowAnyException();
    }

    @Test
    void staffWithoutAgencyIsRejected() {
        assertThatThrownBy(() -> AccessPolicyService.agencyScope(user(8, UserTypeEnum.EMPLOYEE, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static User user(int id, UserTypeEnum role, Agency agency) {
        User user = new User();
        user.setId(id);
        user.setUserType(role);
        user.setAgency(agency);
        return user;
    }

    private static Agency agency(int id) {
        Agency agency = new Agency();
        agency.setId(id);
        return agency;
    }
}
