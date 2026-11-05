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
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.exceptions.common.WorkflowException;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.SeatRepository;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.utilities.pagination.PaginationUtils;

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
                return PageResponse.empty();
            }
            transportId = tour.getTransportation().getId();
            taken = inventoryService.takenSeatIds(tour.getId());
        }
        String search = filter.getKeyword() != null ? filter.getKeyword() : filter.getSearch();
        Specification<Seat> spec = hasTransportId(transportId)
                .and(hasChairType(filter.getChairType()))
                .and((r, q, cb) -> search == null || search.isBlank() ? cb.conjunction()
                        : cb.like(cb.lower(r.get("seatCode")), "%" + search.toLowerCase() + "%"));
        // Filtering in the query keeps page sizes and totals right for the per-tour BOOKED status too.
        spec = spec.and(filter.getTourId() == null ? hasStatus(filter.getStatus()) : hasTourStatus(filter.getStatus(), taken));
        Set<Integer> takenForTour = taken;
        boolean perTour = filter.getTourId() != null;
        return PageResponse.from(seatRepository.findAll(spec,
                PaginationUtils.pageable(filter, "seatCode", SORTABLE, Map.of())))
                .map(seat -> SeatResponse.from(seat).withStatus(perTour ? tourStatus(seat, takenForTour) : seat.getStatus()));
    }

    /** A seat's status for one tour: BOOKED when an active ticket holds it, otherwise its physical condition. */
    private static SeatStatus tourStatus(Seat seat, Set<Integer> taken) {
        if (taken.contains(seat.getId())) {
            return SeatStatus.BOOKED;
        }
        return seat.getStatus() == SeatStatus.MAINTENANCE ? SeatStatus.MAINTENANCE : SeatStatus.AVAILABLE;
    }

    /** Chair type and price can only change while no active booking or approved/active tour uses the seat. */
    @Transactional
    public Seat updateSeat(@NonNull Integer id, SeatUpdateRequest updatedData) {
        Seat seat = getById(id);
        requireEditable(seat);
        boolean layoutChange = updatedData.chairType() != null || updatedData.seatPriceModifier() != null;
        if (layoutChange) {
            requireLayoutUnlocked(seat);
        }
        if (updatedData.chairType() != null) {
            seat.setChairType(updatedData.chairType());
        }
        if (updatedData.seatPriceModifier() != null) {
            seat.setSeatPriceModifier(updatedData.seatPriceModifier());
        }
        if (updatedData.status() != null) {
            applyStatus(seat, updatedData.status());
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

    /** The per-tour status filter expressed as a query: BOOKED means held by the tour's active tickets. */
    private Specification<Seat> hasTourStatus(SeatStatus status, Set<Integer> taken) {
        return (r, q, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            if (status == SeatStatus.BOOKED) {
                return taken.isEmpty() ? cb.disjunction() : r.get("id").in(taken);
            }
            var notTaken = taken.isEmpty() ? cb.conjunction() : cb.not(r.get("id").in(taken));
            return status == SeatStatus.MAINTENANCE
                    ? cb.and(notTaken, cb.equal(r.get("status"), SeatStatus.MAINTENANCE))
                    : cb.and(notTaken, cb.notEqual(r.get("status"), SeatStatus.MAINTENANCE));
        };
    }

    private Specification<Seat> hasChairType(ChairType c) {
        return (r, q, cb) -> c == null ? cb.conjunction() : cb.equal(r.get("chairType"), c);
    }
}
