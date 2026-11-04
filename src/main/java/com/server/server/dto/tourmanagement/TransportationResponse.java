package com.server.server.dto.tourmanagement;

import java.util.List;

import com.server.server.dto.agency.AgencySummary;
import com.server.server.dto.agency.BranchSummary;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.models.tourmanagement.Transportation;
/** @param seats included on the detail view only; lists leave it null to stay light */
public record TransportationResponse(
        Integer id,
        String transportationNumber,
        String code,
        TransportationType type,
        String providerName,
        GenericStatus status,
        TransportationStatus unitStatus,
        Integer totalCapacity,
        AgencySummary agency,
        BranchSummary agencyBranch,
        List<SeatSummary> seats) {

    /** Detail view with the seat list. */
    public static TransportationResponse from(Transportation vehicle) {
        return of(vehicle, true);
    }

    /** List view without seats. */
    public static TransportationResponse listItem(Transportation vehicle) {
        return of(vehicle, false);
    }

    private static TransportationResponse of(Transportation vehicle, boolean includeSeats) {
        if (vehicle == null) {
            return null;
        }
        List<SeatSummary> seats = includeSeats && vehicle.getSeats() != null
                ? vehicle.getSeats().stream().map(SeatSummary::from).toList()
                : null;
        return new TransportationResponse(vehicle.getId(), vehicle.getTransportationNumber(), vehicle.getCode(),
                vehicle.getType(), vehicle.getProviderName(), vehicle.getStatus(), vehicle.getUnitStatus(),
                vehicle.getTotalCapacity(),
                AgencySummary.from(vehicle.getAgency()), BranchSummary.from(vehicle.getAgencyBranch()), seats);
    }
}
