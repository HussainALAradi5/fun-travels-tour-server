package com.server.server.services.report;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.server.server.dto.report.ReportRequest;
import com.server.server.enums.report.ReportFormat;
import com.server.server.models.media.Media;
import com.server.server.models.user.User;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.media.MediaService;
import com.server.server.utilities.report.ExcelReportRenderer;
import com.server.server.utilities.report.PdfReportRenderer;
import com.server.server.utilities.report.ReportCells;
import com.server.server.utilities.report.ReportTable;

import lombok.RequiredArgsConstructor;

/** Generates a branded PDF or Excel report and keeps it as the requester's media. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccessPolicyService accessPolicy;
    private final ReportDataService reportDataService;
    private final MediaService mediaService;

    public Media generate(ReportRequest request) {
        User actor = accessPolicy.requireCurrentUser();
        if (!request.type().allows(actor.getUserType())) {
            throw new AccessDeniedException("You are not allowed to export " + request.type().title() + ".");
        }
        ReportTable table = reportDataService.load(request, actor);
        byte[] content = request.format() == ReportFormat.PDF ? PdfReportRenderer.render(table) : ExcelReportRenderer.render(table);
        String fileName = ReportCells.fileStem(table.title(), table.generatedAt()) + "." + request.format().extension();
        return mediaService.storeReport(content, fileName, request.type(), request.format(), table.rows().size(), actor);
    }
}
