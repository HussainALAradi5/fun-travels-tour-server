package com.server.server.controllers.report;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.media.MediaResponse;
import com.server.server.dto.report.ReportRequest;
import com.server.server.services.report.ReportService;
import com.server.server.utilities.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** Generates the report, stores it as the caller's media, and returns where to download it. */
    @PostMapping
    public ResponseEntity<ApiResponse<MediaResponse>> generate(@Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Report ready", MediaResponse.from(reportService.generate(request))));
    }
}
