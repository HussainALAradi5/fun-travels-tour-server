package com.server.server.dto.tourmanagement;

import java.util.List;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Creates a reservation. Customers always book for themselves; staff pass {@code user}.
 * When {@code tickets} is empty the server issues {@code requestedSlots} tickets without
 * seat or meal choices.
 */
public record ReservationCreateRequest(
        @NotNull(message = "tour is required") @Valid EntityReference tour,
        @Valid EntityReference user,
        @Positive(message = "requestedSlots must be at least 1") @Max(value = MAX_TRAVELERS, message = "A reservation can hold at most 20 travelers") Integer requestedSlots,
        @Size(max = MAX_TRAVELERS, message = "A reservation can hold at most 20 travelers") List<@Valid TicketSelectionRequest> tickets) {

    public static final int MAX_TRAVELERS = 20;
}
