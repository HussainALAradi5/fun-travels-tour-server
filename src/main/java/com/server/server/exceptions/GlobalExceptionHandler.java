package com.server.server.exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowException(WorkflowException ex) {
        // Returns 409 Conflict for workflow violations
        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(errorMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(Exception ex) {
        return buildResponse("Record conflict: Duplicate unique field.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // --- NEW: Deep-Digging Transaction Handler ---
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionSystemException(TransactionSystemException ex) {
        // 1. PRINT THE EXACT CRASH TO CONSOLE SO WE CAN FIND THE LOOP!
        System.err.println("=== TRANSACTION CRASH DETECTED ===");
        ex.printStackTrace(); 
        
        Throwable cause = ex;
        while (cause != null) {
            // Check if a @NotNull or @NotBlank failed right before DB commit
            if (cause instanceof ConstraintViolationException) {
                ConstraintViolationException consEx = (ConstraintViolationException) cause;
                String errorMessage = consEx.getConstraintViolations()
                        .stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .collect(Collectors.joining(", "));
                return buildResponse("Validation Failed -> " + errorMessage, HttpStatus.BAD_REQUEST);
            }
            if (cause.getCause() == cause) break;
            cause = cause.getCause();
        }
        
        // Extract the deepest root cause (usually StackOverflowError)
        Throwable root = ex;
        while(root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        
        return buildResponse("DB Crash (" + root.getClass().getSimpleName() + "): " + root.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        // Print unexpected runtime errors to terminal too
        ex.printStackTrace();
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("status", status.value());
        return new ResponseEntity<>(response, status);
    }
}