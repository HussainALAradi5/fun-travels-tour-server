package com.server.server.dto.tourmanagement;

import java.util.List;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.Valid;
import lombok.Data;

/** One traveler's optional seat and meal choices. Prices are calculated by the server. */
@Data
public class TicketSelectionRequest {
    @Valid
    private EntityReference assignedSeat;
    private List<@Valid EntityReference> selectedMeals;
}
