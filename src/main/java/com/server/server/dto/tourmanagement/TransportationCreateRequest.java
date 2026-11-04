package com.server.server.dto.tourmanagement;

import java.util.Map;

import com.server.server.enums.tourmanagement.TransportationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record TransportationCreateRequest(
        @NotBlank(message = "Transportation number is required") String transportationNumber,
        @NotBlank(message = "Transportation code is required") String code,
        @NotNull(message = "Transportation type is required") TransportationType type,
        @NotBlank(message = "Provider name is required") String providerName,
        @NotNull(message = "Capacity is required") @Min(value = 1, message = "Capacity must be at least 1") Integer totalCapacity,
        @NotNull(message = "Agency is required") Integer agencyId,
        Integer branchId,
        Map<String, Integer> seatConfig) {
}
