package com.server.server.dto.user;

import com.server.server.models.user.User;
/** A user as other records show them: id and display name. */
public record UserSummary(Integer id, String name) {

    public static UserSummary from(User source) {
        return source == null ? null : new UserSummary(source.getId(), source.getName());
    }
}
