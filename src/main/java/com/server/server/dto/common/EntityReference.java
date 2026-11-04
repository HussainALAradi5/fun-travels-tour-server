package com.server.server.dto.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A reference to an existing record by id, sent as {@code { "id": 1 }}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityReference {
    @NotNull(message = "id is required")
    @Positive(message = "id must be positive")
    private Integer id;
}
