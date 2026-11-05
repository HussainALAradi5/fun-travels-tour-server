package com.server.server.dto.user;

import java.time.LocalDateTime;

import com.server.server.dto.agency.AgencySummary;
import com.server.server.dto.agency.BranchSummary;
import com.server.server.dto.payment.AccountResponse;
import com.server.server.enums.user.Gender;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.models.payment.Account;
import com.server.server.models.user.User;
/**
 * Everything the profile page shows in one response: identity, contact, role, agency placement,
 * wallet, and membership date.
 * @param wallet the user's wallet, or null when the account has none yet
 */
public record ProfileResponse(
        Integer id,
        String code,
        String userName,
        String name,
        String email,
        String mobileNumber,
        Integer age,
        Gender gender,
        UserTypeEnum userType,
        String profileImageUrl,
        boolean active,
        AgencySummary agency,
        BranchSummary agencyBranch,
        AccountResponse wallet,
        LocalDateTime memberSince) {

    public static ProfileResponse from(User user, Account wallet) {
        return new ProfileResponse(user.getId(), user.getCode(), user.getUserName(), user.getName(), user.getEmail(),
                user.getMobileNumber(), user.getAge(), user.getGender(), user.getUserType(), user.getProfileImageUrl(), user.isActive(),
                AgencySummary.from(user.getAgency()), BranchSummary.from(user.getAgencyBranch()),
                AccountResponse.from(wallet), user.getCreatedAt());
    }
}
