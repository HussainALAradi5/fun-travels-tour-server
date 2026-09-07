package com.server.server.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.enums.GenericStatus;
import com.server.server.models.Port;
import com.server.server.services.PortService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ports")
@RequiredArgsConstructor
public class PortController {
    private final PortService portService;

    @GetMapping
    public ResponseEntity<List<Port>> getAllActivePorts() {
        return ResponseEntity.ok(portService.getAllActivePorts());
    }

    @PostMapping
    public ResponseEntity<Port> createPort(@RequestBody Port port) {
        return ResponseEntity.ok(portService.createPort(port));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Port> updateStatus(@PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(portService.updateStatus(id, status));
    }
}