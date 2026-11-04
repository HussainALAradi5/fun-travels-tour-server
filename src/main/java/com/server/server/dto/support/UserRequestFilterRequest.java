package com.server.server.dto.support;

import com.server.server.dto.common.GenericFilterRequest;

import com.server.server.enums.support.UserRequestStatus;
import com.server.server.enums.support.UserRequestType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserRequestFilterRequest extends GenericFilterRequest {
    private Integer currentUserId;
    private UserRequestStatus status;
    private UserRequestType type;
    private Integer userIdFilter;
}
