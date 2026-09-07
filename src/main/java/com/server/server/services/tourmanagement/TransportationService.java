package com.server.server.services.tourmanagement;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Transportation;
import com.server.server.repositories.tourmanagement.TransportationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportationService {
    private final TransportationRepository repository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public List<Transportation> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Transportation getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transportation unit not found."));
    }

// Inside TransportationService.java
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Transportation create(Transportation transportation) {
        transportation.setStatus(GenericStatus.ACTIVE);
        transportation.setUnitStatus(TransportationStatus.AVAILABLE);
        transportation.setRemainingSeats(transportation.getTotalCapacity());

        // THE DRY CALL: Auto-generate seats at the exact moment of creation
        if (transportation.getSeats() == null || transportation.getSeats().isEmpty()) {
            transportation.setSeats(generateSeatLayout(transportation, transportation.getSeatConfig()));
        } else {
            for (Seat seat : transportation.getSeats()) {
                seat.setTransportation(transportation);
            }
        }
        return repository.save(transportation);
    }

    // --- THE DRY HELPER METHOD ---
    private List<Seat> generateSeatLayout(Transportation transport, java.util.Map<String, Integer> config) {
        List<Seat> generated = new java.util.ArrayList<>();
        int seatNum = 1;
        int max = transport.getTotalCapacity();

        // 1. Generate Custom Chair Types if requested
        if (config != null) {
            for (java.util.Map.Entry<String, Integer> entry : config.entrySet()) {
                com.server.server.enums.tourmanagement.ChairType type = 
                    com.server.server.enums.tourmanagement.ChairType.valueOf(entry.getKey());
                int count = entry.getValue() != null ? entry.getValue() : 0;
                
                for (int i = 0; i < count; i++) {
                    if (seatNum > max) throw new WorkflowException("Configuration exceeds capacity!");
                    generated.add(buildSeat(transport, seatNum++, type));
                }
            }
        }
        
        // 2. Automatically fill the remaining capacity with STANDARD seats
        while (seatNum <= max) {
            generated.add(buildSeat(transport, seatNum++, com.server.server.enums.tourmanagement.ChairType.STANDARD));
        }
        return generated;
    }

    private Seat buildSeat(Transportation transport, int number, com.server.server.enums.tourmanagement.ChairType type) {
        Seat seat = new Seat();
        seat.setSeatCode("S" + number);
        seat.setChairType(type);
        seat.setStatus(com.server.server.enums.tourmanagement.SeatStatus.AVAILABLE);
        seat.setSeatPriceModifier(0.0);
        seat.setTransportation(transport);
        return seat;
    }
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Transportation update(Integer id, Transportation incomingData) {
        Transportation existing = getById(id);

        // Efficient update ignoring managed relationships and status fields
        BeanUtils.copyProperties(incomingData, existing,
                "id", "status", "unitStatus", "seats", "remainingSeats", "calculatedAvailable", "agency", "tours");

        return repository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public Transportation updateStatus(Integer id, TransportationStatus newUnitStatus) {
        Transportation transport = getById(id);
        validateStatusTransition(transport.getUnitStatus(), newUnitStatus);
        transport.setUnitStatus(newUnitStatus);
        return repository.save(transport);
    }

    private void validateStatusTransition(TransportationStatus current, TransportationStatus target) {
        if (current == TransportationStatus.FULL && target == TransportationStatus.AVAILABLE) {
            throw new WorkflowException(
                    "Cannot force a FULL unit to AVAILABLE. Seats must be freed via ticket cancellations.");
        }
    }

    @Transactional(readOnly = true)
    public List<Transportation> filter(TransportationType type, GenericStatus status, TransportationStatus unitStatus, String keyword) {
        return repository.filterAndSearch(keyword, type, status, unitStatus);
    }

    private Specification<Transportation> hasType(TransportationType t) {
        return (r, q, cb) -> t == null ? cb.conjunction() : cb.equal(r.get("type"), t);
    }

    private Specification<Transportation> hasStatus(GenericStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("status"), s);
    }

    private Specification<Transportation> hasUnitStatus(TransportationStatus us) {
        return (r, q, cb) -> us == null ? cb.conjunction() : cb.equal(r.get("unitStatus"), us);
    }

    private Specification<Transportation> hasProvider(String p) {
        return (r, q, cb) -> (p == null || p.isBlank()) ? cb.conjunction()
                : cb.like(cb.lower(r.get("providerName")), "%" + p.toLowerCase() + "%");
    }
}