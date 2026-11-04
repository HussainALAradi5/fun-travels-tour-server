package com.server.server.repositories.tourmanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.tourmanagement.Ticket;
public interface TicketRepository extends JpaRepository<Ticket, Integer>, JpaSpecificationExecutor<Ticket> {





    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.tour tour " +
            "LEFT JOIN FETCH tour.startCity " +
            "LEFT JOIN FETCH tour.startCountry " +
            "LEFT JOIN FETCH tour.endCountry " +
            "LEFT JOIN FETCH tour.startCity " +
            "LEFT JOIN FETCH tour.endCity " +
            "LEFT JOIN FETCH t.customer " +
            "LEFT JOIN FETCH t.assignedSeat " +
            "LEFT JOIN FETCH t.selectedMeals " +
            "WHERE t.id = :id")
    Optional<Ticket> findByIdWithDetails(@Param("id") Integer id);

    List<Ticket> findByTourIdAndTicketStatus(Integer tourId, TicketStatus status);

    @EntityGraph(attributePaths = { "customer", "assignedSeat", "tour" })
    List<Ticket> findAll(@Nullable Specification<Ticket> spec, @NonNull Sort sort);

    @Override
    @EntityGraph(attributePaths = { "customer", "assignedSeat", "tour" })
    List<Ticket> findAll(@Nullable Specification<Ticket> spec);

@Query("SELECT t FROM Ticket t JOIN t.tour tour " +
           "WHERE tour.startDate <= :date " +
           "AND t.ticketStatus = com.server.server.enums.tourmanagement.TicketStatus.PENDING")
    List<Ticket> findAllPendingByDateBefore(@Param("date") java.time.LocalDate date);

    /** Seats held for a tour by tickets that are not cancelled (pending holds included). */
    @Query("SELECT t.assignedSeat.id FROM Ticket t WHERE t.tour.id = :tourId AND t.assignedSeat IS NOT NULL "
            + "AND t.ticketStatus <> com.server.server.enums.tourmanagement.TicketStatus.CANCELLED")
    List<Integer> findTakenSeatIds(@Param("tourId") Integer tourId);

    /** Whether a seat is held by an active ticket of a tour that has not finished or been cancelled. */
    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.assignedSeat.id = :seatId "
            + "AND t.ticketStatus IN (com.server.server.enums.tourmanagement.TicketStatus.PENDING, "
            + "com.server.server.enums.tourmanagement.TicketStatus.CONFIRMED) "
            + "AND t.tour.status NOT IN (com.server.server.enums.common.GenericStatus.COMPLETED, "
            + "com.server.server.enums.common.GenericStatus.CANCELLED)")
    boolean existsActiveTicketForSeat(@Param("seatId") Integer seatId);

    /** Whether a tour still has tickets that are not cancelled. */
    boolean existsByTour_IdAndTicketStatusNot(Integer tourId, TicketStatus status);
}
