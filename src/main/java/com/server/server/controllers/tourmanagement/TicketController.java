package com.server.server.controllers.tourmanagement;

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
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.services.tourmanagement.TicketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/filter")
    public ResponseEntity<List<Ticket>> filter(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer tourId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return ResponseEntity.ok(ticketService.filter(status, customerId, tourId, sortBy, sortDir));
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getAll() {
        return ResponseEntity.ok(ticketService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Ticket> create(@RequestBody Ticket ticket) {
        // Native Spring Boot binding directly to the entity
        return ResponseEntity.ok(ticketService.create(ticket));
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<Ticket> updateStatus(
            @PathVariable Integer id,
            @RequestParam GenericStatus status) {
        return ResponseEntity.ok(ticketService.updateStatus(id, status));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Ticket> cancel(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.updateStatus(id, GenericStatus.CANCELLED));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Ticket> approve(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.updateStatus(id, GenericStatus.APPROVED));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Ticket> confirm(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.updateStatus(id, GenericStatus.CONFIRMED));
    }

   
}