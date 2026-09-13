package com.server.server.controllers.tourmanagement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.tour.TicketResponse;
import com.server.server.dto.PageResponse;
import com.server.server.dto.filter.TicketFilterRequest;
import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.services.tourmanagement.TicketService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
    private final ModelMapper modelMapper;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> search(@ModelAttribute TicketFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.filter(filter).map(modelMapper::toTicketResponse)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getAll(page, size, sortDir)
                .map(modelMapper::toTicketResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTicketResponse(ticketService.getById(id))));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public ResponseEntity<ApiResponse<TicketResponse>> create(@Valid @RequestBody Ticket ticket) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket created!", modelMapper.toTicketResponse(ticketService.create(ticket))));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTicketResponse(ticketService.updateStatus(id, status))));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public ResponseEntity<ApiResponse<TicketResponse>> cancel(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket cancelled!", modelMapper.toTicketResponse(ticketService.updateStatus(id, GenericStatus.CANCELLED))));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> approve(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket approved!", modelMapper.toTicketResponse(ticketService.updateStatus(id, GenericStatus.APPROVED))));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> confirm(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket confirmed!", modelMapper.toTicketResponse(ticketService.updateStatus(id, GenericStatus.CONFIRMED))));
    }
}
