package com.server.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.server.server.constants.errors.ErrorCodes;
import com.server.server.utilities.ApiResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/missing");

    @Test
    void unknownRouteIsNotFoundNotServerError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknownRoute(
                new NoResourceFoundException(HttpMethod.GET, "api/missing"), request);
        assertError(response, HttpStatus.NOT_FOUND, ErrorCodes.ROUTE_NOT_FOUND);
        assertThat(response.getBody().getPath()).isEqualTo("/api/missing");
    }

    @Test
    void wrongMethodIsMethodNotAllowed() {
        assertError(handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("DELETE"), request),
                HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.METHOD_NOT_ALLOWED);
    }

    @Test
    void missingRecordIsNotFound() {
        assertError(handler.handleNotFound(new ResourceNotFoundException("Port", 7), request),
                HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND);
    }

    @Test
    void storageFailureHidesTheCause() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleStorage(
                new StorageException("Could not save the file.", new IOException("disk full at D:/media")), request);
        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.STORAGE_FAILED);
        assertThat(response.getBody().getMessage()).doesNotContain("disk full");
    }

    @Test
    void externalFailureHidesConfiguration() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleExternal(
                new ExternalServiceException("REST_COUNTRIES_API_KEY is not configured"), request);
        assertError(response, HttpStatus.BAD_GATEWAY, ErrorCodes.EXTERNAL_SERVICE_FAILED);
        assertThat(response.getBody().getMessage()).doesNotContain("REST_COUNTRIES_API_KEY");
    }

    @Test
    void workflowKeepsItsOwnCode() {
        assertError(handler.handleWorkflow(new WorkflowException("SEAT_TAKEN", "Seat taken"), request),
                HttpStatus.CONFLICT, "SEAT_TAKEN");
    }

    private static void assertError(ResponseEntity<ApiResponse<Void>> response, HttpStatus status, String code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo(code);
    }
}
