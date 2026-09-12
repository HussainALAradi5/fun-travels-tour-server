package com.server.server.services.tourmanagement;

import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.dto.filter.SeatFilterRequest;
import com.server.server.dto.PageResponse;
import com.server.server.utilities.PaginationUtils;
import java.util.Set;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.repositories.tourmanagement.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public PageResponse<Seat> getAll(Integer page, Integer size, String sortDir) {
        return PageResponse.from(seatRepository.findAll(PaginationUtils.pageable(page, size, "seatCode", sortDir,
                "seatCode", Set.of("seatCode"))));
    }

    @Transactional(readOnly = true)
    public Seat getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return seatRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Seat not found"));
    }


@Transactional
    public Seat updateSeat(@NonNull Integer id, Seat updatedData) {
        Objects.requireNonNull(id, "id must not be null");
        Seat seat = getById(id);
        
        // --- STRICT RULE: Cannot modify if booked ---
        if (updatedData.getChairType() != null && seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new com.server.server.exceptions.WorkflowException("Cannot change the chair type of a booked seat.");
        }

        // --- STRICT RULE: Cannot modify if any linked tour is APPROVED ---
        if (seat.getTransportation() != null && seat.getTransportation().getTours() != null) {
            boolean isLocked = seat.getTransportation().getTours().stream()
                .anyMatch(tour -> tour.getStatus() == com.server.server.enums.GenericStatus.APPROVED);
            
            if (isLocked) {
                throw new com.server.server.exceptions.WorkflowException("Seat layout is locked. The vehicle is assigned to an APPROVED tour.");
            }
        }
        
        if (updatedData.getChairType() != null) seat.setChairType(updatedData.getChairType());
        if (updatedData.getStatus() != null) seat.setStatus(updatedData.getStatus());
        if (updatedData.getSeatPriceModifier() != null) seat.setSeatPriceModifier(updatedData.getSeatPriceModifier());
        
        return seatRepository.save(seat);
    }
    
    @Transactional(readOnly = true)
    public PageResponse<Seat> filter(SeatFilterRequest filter) {
        String search = filter.getKeyword() != null ? filter.getKeyword() : filter.getSearch();
        Specification<Seat> spec = Specification.where(hasTransportId(filter.getTransportId()))
                .and(hasStatus(filter.getStatus())).and(hasChairType(filter.getChairType()))
                .and((r, q, cb) -> search == null || search.isBlank() ? cb.conjunction()
                        : cb.like(cb.lower(r.get("seatCode")), "%" + search.toLowerCase() + "%"));
        return PageResponse.from(seatRepository.findAll(spec, PaginationUtils.pageable(filter,
                "seatCode", Set.of("id", "seatCode", "chairType", "status", "seatPriceModifier"), java.util.Map.of())));
    }
    
    @Transactional
    public Seat updateStatus(@NonNull Integer id, SeatStatus status) {
        Objects.requireNonNull(id, "id must not be null");
        Seat seat = getById(id);
        seat.setStatus(status);
        return seatRepository.save(seat);
    }

    @Transactional(readOnly = true)
    public List<Seat> filter(Integer transportId, SeatStatus status, ChairType chairType) {
        return seatRepository.findAll(Specification.where(hasTransportId(transportId))
                .and(hasStatus(status))
                .and(hasChairType(chairType)));
    }

    private Specification<Seat> hasTransportId(Integer tId) { return (r, q, cb) -> tId == null ? cb.conjunction() : cb.equal(r.get("transportation").get("id"), tId); }
    private Specification<Seat> hasStatus(SeatStatus s) { return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("status"), s); }
    private Specification<Seat> hasChairType(ChairType c) { return (r, q, cb) -> c == null ? cb.conjunction() : cb.equal(r.get("chairType"), c); }
}
