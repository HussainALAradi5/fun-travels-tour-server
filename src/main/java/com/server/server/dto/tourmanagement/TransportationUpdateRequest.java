package com.server.server.dto.tourmanagement;

import java.util.Map;

import com.server.server.enums.tourmanagement.TransportationType;

import lombok.Data;

/** Editable vehicle fields. Code and capacity are permanent; {@code seatConfig} re-labels chair types. */
@Data
public class TransportationUpdateRequest {
    private String transportationNumber;
    private String providerName;
    private TransportationType type;
    private Map<String, Integer> seatConfig;
}
