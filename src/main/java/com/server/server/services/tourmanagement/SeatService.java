package com.server.server.services.tourmanagement;

import com.server.server.dto.tourmanagement.SeatUpdateRequest;

import java.util.Map;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.dto.common.PageResponse;
import com.server.server.dto.tourmanagement.SeatFilterRequest;
import com.server.server.dto.tourmanagement.SeatResponse;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.SeatRepository;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.utilities.ModelMapper;
import com.server.server.utilities.PaginationUtils;

import lombok.RequiredArgsConstructor;

/**
 * Seat records of a vehicle. A seat's stored status is its physical condition
 * ({@code AVAILABLE} or {@code MAINTENANCE}); whether it is sold is answered per tour.
 */
@Service
@RequiredArgsConstructor
public class SeatService {

    private static final Set<String> SORTABLE = Set.of("id", "seatCode", "chairType", "status", "seatPriceModifier");

    private final SeatRepository seatRepository;
    private final TourRepository tourRepository;
    private final InventoryService inventoryService;
    private final AccessPolicyService accessPolicy;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public PageResponse<Seat> getAll(Integer page, Integer size, String sortDir) {
        return PageResponse.from(seatRepository.findAll(PaginationUtils.pageable(page, size, "seatCode", sortDir,
                "seatCode", Set.of("seatCode"))));
    }

    @Transactional(readOnly = true)
    public Seat getById(@NonNull Integer id) {
        return seatRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seat", id));
    }

    /**
     * Lists seats. With {@code tourId} the seats of that tour's vehicle are returned and seats held
     * by the tour's active tickets are reported as {@code BOOKED}.
     */
    @Transactional(readOnly = true)
    public PageResponse<SeatResponse> search(SeatFilterRequest filter) {
        Set<Integer> taken = Set.of();
        Integer transportId = filter.getTransportId();
        if (filter.getTourId() != null) {
            Tour tour = tourRepository.findById(filter.getTourId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tour", filter.getTourId()));
            if (tour.getTransportation() == null) {
                return new PageResponse<>(java.util.List.of(), 0, 0, 0, 0);
            }
            transportId = tour.getTransportation().getId();
            taken = inventoryService.takenSeatIds(tour.getId());
        }
        String search = filter.getKeyword() != null ? filter.getKeyword() : filter.getSearch();
        Specification<Seat> spec = hasTransportId(transportId)
                .and(hasChairType(filter.getChairType()))
                .and((r, q, cb) -> search == null || search.isBlank() ? cb.conjunction()
                        : cb.like(cb.lower(r.get("seatCode")), "%" + search.toLowerCase() + "%"));
        if (filter.getTourId() == null) {
            spec = spec.and(hasStatus(filter.getStatus()));
        }
        Set<Integer> takenForTour = taken;
        PageResponse<SeatResponse> page = PageResponse.from(seatRepository.findAll(spec,
                PaginationUtils.pageable(filter, "seatCode", SORTABLE, Map.of()))).map(seat -> {
                    SeatResponse response = modelMapper.toSeatResponse(seat);
                    if (takenForTour.contains(seat.getId())) {
                        response.setStatus(SeatStatus.BOOKED);
                    } else if (response.getStatus() != SeatStatus.MAINTENANCE) {
                        response.setStatus(SeatStatus.AVAILABLE);
                    }
                    return response;
                });
        if (filter.getTourId() != null && filter.getStatus() != null) {
            page = new PageResponse<>(page.getContent().stream()
                    .filter(seat -> seat.getStatus() == filter.getStatus()).toList(),
                    page.getPage(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
        return page;
    }

    /** Chair type and price can only change while no active booking or approved/active tour uses the seat. */
    @Transactional
    public Seat updateSeat(@NonNull Integer id, SeatUpdateRequest updatedData) {
        Seat seat = getById(id);
        requireEditable(seat);
        boolean layoutChange = updatedData.getChairType() != null || updatedData.getSeatPriceModifier() != null;
        if (layoutChange) {
            requireLayoutUnlocked(seat);
        }
        if (updatedData.getChairType() != null) {
            seat.setChairType(updatedData.getChairType());
        }
        if (updatedData.getSeatPriceModifier() != null) {
            seat.setSeatPriceModifier(updatedData.getSeatPriceModifier());
        }
        if (updatedData.getStatus() != null) {
            applyStatus(seat, updatedData.getStatus());
        }
        return seatRepository.save(seat);
    }

    @Transactional
    public Seat updateStatus(@NonNull Integer id, SeatStatus status) {
        Seat seat = getById(id);
        requireEditable(seat);
        applyStatus(seat, status);
        return seatRepository.save(seat);
    }

    /** Staff only toggle AVAILABLE and MAINTENANCE; a seat held by an active ticket cannot go to maintenance. */
    private void applyStatus(Seat seat, SeatStatus target) {
        if (target != SeatStatus.AVAILABLE && target != SeatStatus.MAINTENANCE) {
            throw new WorkflowException("SEAT_STATUS_LOCKED",
                    "Seats are reserved and booked through tickets; staff can only set AVAILABLE or MAINTENANCE.");
        }
        if (target == SeatStatus.MAINTENANCE && inventoryService.isSeatInUse(seat.getId())) {
            throw new WorkflowException("SEAT_IN_USE", "This seat is held by an active ticket. Move or cancel the ticket first.");
        }
        seat.setStatus(target);
    }

    private void requireEditable(Seat seat) {
        accessPolicy.requireAgencyAccess(accessPolicy.requireCurrentUser(),
                seat.getTransportation() == null ? null : seat.getTransportation().getAgency());
    }

    private void requireLayoutUnlocked(Seat seat) {
        if (inventoryService.isSeatInUse(seat.getId())) {
            throw new WorkflowException("SEAT_IN_USE", "Cannot change the type or price of a seat held by an active ticket.");
        }
        if (seat.getTransportation() != null && seat.getTransportation().getTours() != null
                && seat.getTransportation().getTours().stream().anyMatch(tour ->
                        tour.getStatus() == GenericStatus.APPROVED || tour.getStatus() == GenericStatus.ACTIVE)) {
            throw new WorkflowException("SEAT_LAYOUT_LOCKED",
                    "Seat layout is locked while the vehicle is assigned to an approved or active tour.");
        }
    }

    private Specification<Seat> hasTransportId(Integer tId) {
        return (r, q, cb) -> tId == null ? cb.conjunction() : cb.equal(r.get("transportation").get("id"), tId);
    }

    private Specification<Seat> hasStatus(SeatStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("status"), s);
    }

    private Specification<Seat> hasChairType(ChairType c) {
        return (r, q, cb) -> c == null ? cb.conjunction() : cb.equal(r.get("chairType"), c);
    }
}
