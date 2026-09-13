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

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.tourmanagement.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Integer>, JpaSpecificationExecutor<Ticket> {
    List<Ticket> findByTicketStatus(TicketStatus status);

    List<Ticket> findByApprovalStatus(GenericStatus status);

    List<Ticket> findByCustomer_Id(Integer customerId);

    Ticket findByTicketNumber(String ticketNumber);

    List<Ticket> findByReservation_Id(Integer reservationId);

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
           "AND t.ticketStatus = TicketStatus.PENDING")
    List<Ticket> findAllPendingByDateBefore(@Param("date") java.time.LocalDate date);
    
}
