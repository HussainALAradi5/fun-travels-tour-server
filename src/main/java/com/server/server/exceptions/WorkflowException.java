package com.server.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WorkflowException extends RuntimeException {
    public WorkflowException(String message) {
        super(message);
    }
}