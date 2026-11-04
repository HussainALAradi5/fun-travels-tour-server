package com.server.server.dto.user;

import com.server.server.models.user.User;
/** A user with their email, for records where contacting them is the point (agency owner). */
public record UserContactSummary(Integer id, String name, String email) {

    public static UserContactSummary from(User source) {
        return source == null ? null : new UserContactSummary(source.getId(), source.getName(), source.getEmail());
    }
}
