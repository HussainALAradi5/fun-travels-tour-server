package com.server.server.dto.tourmanagement;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Staff-issued single ticket outside a reservation (walk-in sale). */
@Data
@EqualsAndHashCode(callSuper = false)
public class TicketCreateRequest extends TicketSelectionRequest {
    @NotNull(message = "tour is required")
    @Valid
    private EntityReference tour;

    @NotNull(message = "customer is required")
    @Valid
    private EntityReference customer;
}
