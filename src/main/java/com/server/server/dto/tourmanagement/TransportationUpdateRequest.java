package com.server.server.dto.tourmanagement;

import java.util.Map;

import com.server.server.enums.tourmanagement.TransportationType;

/** Editable vehicle fields. Code and capacity are permanent; {@code seatConfig} re-labels chair types. */
public record TransportationUpdateRequest(
        String transportationNumber,
        String providerName,
        TransportationType type,
        Map<String, Integer> seatConfig) {
}
