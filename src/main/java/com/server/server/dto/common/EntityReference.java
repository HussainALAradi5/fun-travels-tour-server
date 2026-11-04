package com.server.server.dto.common;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** A reference to an existing record by id, sent as {@code { "id": 1 }}. */
public record EntityReference(
        @NotNull(message = "id is required") @Positive(message = "id must be positive") Integer id) {
}
