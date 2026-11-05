package com.server.server.dto.media;

import java.time.LocalDateTime;

import com.server.server.enums.media.MediaCategory;
import com.server.server.enums.report.ReportFormat;
import com.server.server.enums.report.ReportType;
import com.server.server.models.media.Media;

/**
 * A stored file as clients see it; the storage path stays on the server.
 * @param downloadPath path under the API base URL (for example {@code /media/7/download}) that streams the file to its owner or an administrator
 */
public record MediaResponse(
        Integer id,
        String code,
        String fileName,
        String contentType,
        long sizeBytes,
        MediaCategory category,
        ReportType reportType,
        ReportFormat reportFormat,
        Integer recordCount,
        LocalDateTime createdAt,
        String downloadPath) {

    public static MediaResponse from(Media media) {
        if (media == null) {
            return null;
        }
        return new MediaResponse(media.getId(), media.getCode(), media.getFileName(), media.getContentType(),
                media.getSizeBytes(), media.getCategory(), media.getReportType(), media.getReportFormat(),
                media.getRecordCount(), media.getCreatedAt(), "/media/" + media.getId() + "/download");
    }
}
