package com.server.server.constants.errors;

/** Error codes in the {@code code} field of every failed response; clients branch on these, not on messages. */
public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
    public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";
    public static final String CONCURRENT_UPDATE = "CONCURRENT_UPDATE";
    public static final String CONFLICT = "CONFLICT";
    public static final String WORKFLOW_CONFLICT = "WORKFLOW_CONFLICT";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
    public static final String STORAGE_FAILED = "STORAGE_FAILED";
    public static final String EXTERNAL_SERVICE_FAILED = "EXTERNAL_SERVICE_FAILED";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
    }
}
