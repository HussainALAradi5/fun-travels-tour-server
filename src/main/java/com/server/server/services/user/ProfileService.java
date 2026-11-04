package com.server.server.services.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.dto.user.ProfileResponse;
import com.server.server.dto.user.UserUpdateRequest;
import com.server.server.models.user.User;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.payment.AccountService;

import lombok.RequiredArgsConstructor;

/** The signed-in user's own profile: identity, placement, and wallet in one view. */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AccessPolicyService accessPolicy;
    private final UserService userService;
    private final AccountService accountService;

    @Transactional
    public ProfileResponse getMyProfile() {
        return toProfile(accessPolicy.requireCurrentUser());
    }

    /** Name, mobile number, age, and profile image; role, agency, email, and password change elsewhere. */
    @Transactional
    public ProfileResponse updateMyProfile(UserUpdateRequest request) {
        User current = accessPolicy.requireCurrentUser();
        return toProfile(userService.updateProfile(current.getId(), request));
    }

    private ProfileResponse toProfile(User user) {
        // Creates the wallet when an older account has none, so the profile always shows one.
        return ProfileResponse.from(user, accountService.getAccountByUserId(user.getId()));
    }
}
