package com.server.server.dto.tour;

import java.util.Map;

import com.server.server.enums.tourmanagement.TransportationType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransportationCreateRequest {
    @NotBlank(message = "Transportation number is required")
    private String transportationNumber;
    @NotBlank(message = "Transportation code is required")
    private String code;
    @NotNull(message = "Transportation type is required")
    private TransportationType type;
    @NotBlank(message = "Provider name is required")
    private String providerName;
    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer totalCapacity;
    @NotNull(message = "Agency is required")
    private Integer agencyId;
    private Integer branchId;
    private Map<String, Integer> seatConfig;
}
