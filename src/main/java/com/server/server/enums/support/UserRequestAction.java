package com.server.server.enums.support;

/**
 * Commands that can change the lifecycle of a user support request.
 *
 * Keep these serialized names aligned with the client UserRequestAction type.
 */
public enum UserRequestAction {
    ASSIGN,
    SOLVE,
    REJECT
}
