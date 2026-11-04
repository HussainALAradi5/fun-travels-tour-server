package com.server.server.dto.geography;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CityCreateRequest(
        @NotBlank(message = "City name is required") String name,
        @NotNull(message = "Country is required") @Valid EntityReference country) {
}
