package com.server.server.dto.tourmanagement;

import java.util.List;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Creates a reservation. Customers always book for themselves; staff pass {@code user}.
 * When {@code tickets} is empty the server issues {@code requestedSlots} tickets without
 * seat or meal choices.
 */
@Data
public class ReservationCreateRequest {
    public static final int MAX_TRAVELERS = 20;

    @NotNull(message = "tour is required")
    @Valid
    private EntityReference tour;

    @Valid
    private EntityReference user;

    @Positive(message = "requestedSlots must be at least 1")
    @Max(value = MAX_TRAVELERS, message = "A reservation can hold at most 20 travelers")
    private Integer requestedSlots;

    @Size(max = MAX_TRAVELERS, message = "A reservation can hold at most 20 travelers")
    private List<@Valid TicketSelectionRequest> tickets;
}
