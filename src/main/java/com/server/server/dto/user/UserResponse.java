package com.server.server.dto.user;

import com.server.server.dto.agency.AgencySummary;
import com.server.server.dto.agency.BranchSummary;
import com.server.server.enums.user.Gender;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.models.user.User;
/** A user account as lists and sign-in return it. */
public record UserResponse(
        Integer id,
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
        BranchSummary agencyBranch) {

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.getId(), user.getUserName(), user.getName(), user.getEmail(),
                user.getMobileNumber(), user.getAge(), user.getGender(), user.getUserType(), user.getProfileImageUrl(), user.isActive(),
                AgencySummary.from(user.getAgency()), BranchSummary.from(user.getAgencyBranch()));
    }
}
