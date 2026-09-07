package com.server.server.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.enums.Notification.ReferenceType;
import com.server.server.services.GenericTrackingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GenericTrackingController {

    private final GenericTrackingService trackingService;

    // --- FETCH EVERYTHING FOR THE TIMELINE ---
    @GetMapping("/{refType}/{refId}")
    public ResponseEntity<?> getTimeline(
            @PathVariable ReferenceType refType, 
            @PathVariable Integer refId) {
        
        // Let the service handle the map building
        Map<String, Object> timelineData = trackingService.getTimelineMap(refId, refType);
        
        return ResponseEntity.ok(Map.of("success", true, "data", timelineData));
    }

    // --- ADD A COMMENT ---
    @PostMapping("/{refType}/{refId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable ReferenceType refType, 
            @PathVariable Integer refId,
            @RequestParam Integer authorId, 
            @RequestBody Map<String, String> payload) {
        
        try {
            // Let the service handle the user lookup and saving
            Object savedComment = trackingService.addComment(refId, refType, payload.get("content"), authorId);
            return ResponseEntity.ok(Map.of("success", true, "data", savedComment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- EDIT A COMMENT ---
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Integer commentId,
            @RequestParam Integer editorId, 
            @RequestBody Map<String, String> payload) {
        
        try {
            Object updatedComment = trackingService.updateComment(commentId, editorId, payload.get("content"));
            return ResponseEntity.ok(Map.of("success", true, "data", updatedComment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}