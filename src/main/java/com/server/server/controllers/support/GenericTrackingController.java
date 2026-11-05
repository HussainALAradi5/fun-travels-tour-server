package com.server.server.controllers.support;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.support.CommentRequest;
import com.server.server.dto.support.CommentResponse;
import com.server.server.dto.support.TimelineResponse;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.services.support.GenericTrackingService;
import com.server.server.utilities.ApiResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class GenericTrackingController {

    private final GenericTrackingService trackingService;

    /** Events and comments of one record, oldest first. */
    @GetMapping("/{refType}/{refId}")
    public ResponseEntity<ApiResponse<TimelineResponse>> getTimeline(
            @PathVariable ReferenceType refType, 
            @NonNull @PathVariable Integer refId) {
        
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTimeline(refId, refType)));
    }

    @PostMapping("/{refType}/{refId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable ReferenceType refType, 
            @NonNull @PathVariable Integer refId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = CommentResponse.from(
                trackingService.addComment(refId, refType, request.content()));
        return ResponseEntity.ok(ApiResponse.ok("Comment added successfully.", response));
    }

    /** Authors edit their own comments. */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @NonNull @PathVariable Integer commentId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = CommentResponse.from(
                trackingService.updateComment(commentId, request.content()));
        return ResponseEntity.ok(ApiResponse.ok("Comment updated successfully.", response));
    }
}
