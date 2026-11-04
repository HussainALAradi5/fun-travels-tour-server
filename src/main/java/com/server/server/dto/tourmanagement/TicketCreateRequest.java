package com.server.server.dto.tourmanagement;

import java.util.List;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Staff-issued single ticket outside a reservation (walk-in sale), with the traveler's optional seat and meals. */
public record TicketCreateRequest(
        @NotNull(message = "tour is required") @Valid EntityReference tour,
        @NotNull(message = "customer is required") @Valid EntityReference customer,
        @Valid EntityReference assignedSeat,
        List<@Valid EntityReference> selectedMeals) {

    /** The seat and meal choices, in the shape reservations use. */
    public TicketSelectionRequest selection() {
        return new TicketSelectionRequest(assignedSeat, selectedMeals);
    }
}
