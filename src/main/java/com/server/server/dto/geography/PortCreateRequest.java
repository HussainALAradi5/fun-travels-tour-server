package com.server.server.dto.geography;

import com.server.server.dto.common.EntityReference;
import com.server.server.enums.geography.PortType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortCreateRequest {
    @NotBlank(message = "Port name is required")
    private String portName;
    private String portCode;
    @NotNull(message = "Port type is required")
    private PortType portType;
    @NotNull(message = "City is required")
    @Valid
    private EntityReference city;
    @NotNull(message = "Country is required")
    @Valid
    private EntityReference country;
    @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
    private Double latitude;
    @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
    private Double longitude;
}
