package com.server.server.controllers.media;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.common.PageResponse;
import com.server.server.services.media.MediaFile;
import com.server.server.dto.media.MediaResponse;
import com.server.server.enums.media.MediaCategory;
import com.server.server.services.media.MediaService;
import com.server.server.utilities.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /** The caller's own files, newest first. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MediaResponse>>> listMine(
            @RequestParam(required = false) MediaCategory category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(ApiResponse.ok(mediaService.listMine(category, page, size).map(MediaResponse::from)));
    }

    /** Streams a file to its owner or an administrator. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@NonNull @PathVariable Integer id) {
        MediaFile file = mediaService.open(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.content());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@NonNull @PathVariable Integer id) {
        mediaService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("File deleted"));
    }
}
