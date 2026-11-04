package com.server.server.dto.tourmanagement;

import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.models.tourmanagement.Transportation;
/** The vehicle assigned to a tour. */
public record TransportationSummary(Integer id, String transportationNumber, TransportationType type) {

    public static TransportationSummary from(Transportation source) {
        return source == null ? null : new TransportationSummary(source.getId(), source.getTransportationNumber(), source.getType());
    }
}
