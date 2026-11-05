package com.server.server.dto.agency;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** New discount and cancellation rules for an agency; the partial-refund window must start no later than the full one. */
public record AgencyPolicyUpdateRequest(
        @NotNull(message = "Kids discount is required") @Min(value = 0, message = "Kids discount cannot be negative")
        @Max(value = 100, message = "Kids discount cannot exceed 100%") Integer kidsDiscountPercent,
        @NotNull(message = "Full refund days are required") @Min(value = 0, message = "Days cannot be negative")
        @Max(value = 365, message = "Use at most 365 days") Integer fullRefundDays,
        @NotNull(message = "Partial refund days are required") @Min(value = 0, message = "Days cannot be negative")
        @Max(value = 365, message = "Use at most 365 days") Integer partialRefundDays,
        @NotNull(message = "Partial refund percent is required") @Min(value = 0, message = "Refund cannot be negative")
        @Max(value = 100, message = "Refund cannot exceed 100%") Integer partialRefundPercent) {
}
