package com.server.server.exceptions.common;

/** A third-party service is not configured or did not answer usefully (HTTP 502, {@code EXTERNAL_SERVICE_FAILED}). */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
