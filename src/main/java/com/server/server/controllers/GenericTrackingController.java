package com.server.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.services.GenericTrackingService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GenericTrackingController {

    private final GenericTrackingService trackingService;
    private final ModelMapper modelMapper;

    // --- FETCH EVERYTHING FOR THE TIMELINE ---
    @GetMapping("/{refType}/{refId}")
    public ResponseEntity<ApiResponse<TimelineResponse>> getTimeline(
            @PathVariable ReferenceType refType, 
            @NonNull @PathVariable Integer refId) {
        
        TimelineResponse timeline = new TimelineResponse(
                trackingService.getEvents(refId, refType).stream().map(modelMapper::toEventLogResponse).toList(),
                trackingService.getComments(refId, refType).stream().map(modelMapper::toCommentResponse).toList());
        return ResponseEntity.ok(ApiResponse.ok(timeline));
    }

    // --- ADD A COMMENT ---
    @PostMapping("/{refType}/{refId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable ReferenceType refType, 
            @NonNull @PathVariable Integer refId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = modelMapper.toCommentResponse(
                trackingService.addComment(refId, refType, request.getContent()));
        return ResponseEntity.ok(ApiResponse.ok("Comment added successfully.", response));
    }

    // --- EDIT A COMMENT ---
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @NonNull @PathVariable Integer commentId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = modelMapper.toCommentResponse(
                trackingService.updateComment(commentId, request.getContent()));
        return ResponseEntity.ok(ApiResponse.ok("Comment updated successfully.", response));
    }
}
