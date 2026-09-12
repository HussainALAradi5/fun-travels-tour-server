package com.server.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WorkflowException extends RuntimeException {
    private final String code;

    public WorkflowException(String message) {
        this("WORKFLOW_CONFLICT", message);
    }

    public WorkflowException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
