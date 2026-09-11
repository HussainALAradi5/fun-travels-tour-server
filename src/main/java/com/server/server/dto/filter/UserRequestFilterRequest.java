package com.server.server.dto.filter;

import com.server.server.enums.UserRequest.UserRequestStatus;
import com.server.server.enums.UserRequest.UserRequestType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserRequestFilterRequest extends GenericFilterRequest {
    private Integer currentUserId;
    private UserRequestStatus status;
    private UserRequestType type;
    private Integer userIdFilter;
}
