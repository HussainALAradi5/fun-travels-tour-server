package com.server.server.dto.report;

import com.server.server.enums.report.ReportFormat;
import com.server.server.enums.report.ReportType;
import com.server.server.enums.user.UserTypeEnum;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * What to export. {@code tourId} is required for the passenger manifest, {@code agencyId} for agency
 * branches and employees; {@code role} narrows the users report.
 */
public record ReportRequest(
        @NotNull(message = "Report type is required") ReportType type,
        @NotNull(message = "Format is required") ReportFormat format,
        @Positive(message = "tourId must be positive") Integer tourId,
        @Positive(message = "agencyId must be positive") Integer agencyId,
        UserTypeEnum role) {
}
