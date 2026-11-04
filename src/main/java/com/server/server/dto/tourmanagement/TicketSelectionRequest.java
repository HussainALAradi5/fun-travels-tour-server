package com.server.server.dto.tourmanagement;

import java.util.List;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.Valid;

/** One traveler's optional seat and meal choices. Prices are calculated by the server. */
public record TicketSelectionRequest(
        @Valid EntityReference assignedSeat,
        List<@Valid EntityReference> selectedMeals) {

    /** A traveler with no seat or meal preference. */
    public static TicketSelectionRequest none() {
        return new TicketSelectionRequest(null, null);
    }
}
